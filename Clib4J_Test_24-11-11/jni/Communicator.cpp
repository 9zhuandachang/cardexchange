#include <iostream>
#include <vector>
#include <numeric>
#include <string>
#include <mutex>
#include <thread>

#include <windows.h>

/*
    --single 64KiB swap space--
0KiB    -----------------------
        struct stSwapDataHeader
    0B      flagLock    [bool]
    1B      DataType    [__int32]
    5B      Op          [__int32] 
    9B      uiSession   [unsigned __int32]
    17B     u64Size     [unsigned __int64]
    18B     ...

1KiB    -----------------------
        [zero terminated string ASCII]
64KiB   -----------------------
*/
#define _SWAP_MEM_NAME      "Global\\CardMtchrSwapMem"
#define _SWAP_MEM_SIZE      64 * 1024   // 64KiB swap memory size, with 1KiB reserved, to contain XML parsed 96 * 128 desc std::vector. Totaly 64*2 = 128KiB for full duplex.

#define SWAP_LOCKED         true
#define SWAP_N_LOCKED       false
#define SWAP_WRITEABLE      false
#define SWAP_N_WRITEABLE    true
#define SWAP_DATA_READY     true
#define SWAP_DATA_N_NREADY  false

// Operation codes, matcher to serverlet
#define OP_NO_OP            0           // Reserved
#define OP_REQ_INIT_DATA    1           // Request data in DB from serverlet to initilize canditate list
#define OP_RESPONSE         2           // Response to a query, one ID for each linew

// Operation codes, serverlet to matcher
#define OP_SVR_NO_OP        0 << 8      // Reserved
#define OP_CRT_DES          1 << 8      // Request to create descriptor for a picture.          With TYP_ID_W_PTH
#define OP_QUERY            2 << 8      // Query a picture with it's descriptor or path.        With TYP_ID_W_DES(_EX) or TYP_ID_W_PTH(_EX)
#define OP_END_OF_DB_ITEM   3 << 8      // The lase message send to matcher on init.            With TYP_DEFAULT
#define OP_NEW_ITEM         4 << 8      // Add a new item to candidate list,                    With TYP_ID_W_DES(_EX) or TYP_ID_W_PTH(_EX)
#define OP_MTCHR_SHTDWN     254 << 8    // Terminate matcher program                            With TYP_DEFAULT
#define OP_MTCHR_REST       255 << 8    // Refresh matcher program state                        With TYP_DEFAULT

// Data types
#define TYP_DEFAULT         0           // Reserved
#define TYP_ID_W_DES        1           // ID - descriptor pair     Line1: ID, Rest: XML-parsed descriptor      With OP_QUERY
#define TYP_ID_W_PTH        2           // ID - file path pair      Line1: ID, Line2: file path                 With OP_CRT_DES or OP_QUERY
#define TYP_ID_W_DES_EX     3           // ID - descriptor pair     Line1: ID, Line2: max IDs in response, Rest: XML-parsed descriptor      With OP_QUERY
#define TYP_ID_W_PTH_EX     4           // ID - file path pair      Line1: ID, Line2: max IDs in response, Line3: file path                 With OP_QUERY

#define TYP_IDS             5           // List of ID, one for each line                                        With OP_RESPONSE

#define TYP_STR             1 << 8      // Zero terminated string. For internal struture: data in stQueue
    

class ServerletCommunicator
{
public:

private:
    std::thread thSender,   // Sender
                thRecvr,    // Recvier
                thTskr;     // Task distributer
    
    bool        flagSenderAlive,
                flagRecvrAlive,
                flagTskrAlive;


#define SIDE_A  true
#define SIDE_B  false

    bool flagClientSide = SIDE_A;       // Side A if not defined in constructor
    

    HANDLE hMapFile     = NULL;
    LPVOID pSwapMapped  = NULL;

    /// @brief Header of data in the swap mem
    struct stSwapDataHeader
    {
        bool                flagLock;   // 1 for data is valid (that is, "data is ready to be read"), locking write. Expected to be witten to shared mem the latest. Should be set to 0 after receiver gathered data.
        __int32             DataType;   // Defined by code elseware.
        __int32             Op;         // Operaton requested, defined elsewere. For simplicity not merged with DataType into one int32.
        unsigned __int32    uiSession;  // Session ID, generated serverlet side.
        unsigned __int64    u64Size;    // String length in char.

        stSwapDataHeader ()
        {
            this->flagLock  = false;
            this->DataType  = 0;
            this->Op        = 0;
            this->u64Size   = 0;
        }

    };


    // Definitions of data strtures for handling queueing
    /// @brief 
    struct stQueueData
    {
        __int32             DataType;   // Data type, differs from types that are used in header, specifically string here.
        __int32             Op;
        unsigned __int32    uiSession;  // Session ID from serverlet side.
        void               *pData;      // Raw string data which corresponds to 1~64KiB part in swap mem. 
                                        // Note that for each object, it's expected to be allocated by malloc() and released by free()
    };

    std::vector<stQueueData> InQueue;
    /// @brief Queue to hold data to be send. void*: data to be send.
    std::vector<stQueueData> OutQueue;

    std::mutex lockInQue;
    std::mutex lockOutQue;


    void SenderThreadFn ()
    {
        /// @todo

        LPVOID              pSenderSwap = (__int8*)pSwapMapped + (flagClientSide ? 0 : _SWAP_MEM_SIZE);   // Sender (matcher side) write to low half of shaed memory
        stSwapDataHeader   &Header      = *(stSwapDataHeader*)pSenderSwap;
        // Similar code chunk in constructor and RecviverThreadFn()

        while (this->flagSenderAlive)
        {
std::this_thread::sleep_for(std::chrono::milliseconds(1));

            while (Header.flagLock != SWAP_WRITEABLE);  // wait

            /// @todo change while() wait to mutex

            if (OutQueue.empty() == false)
            {
                lockOutQue.lock();
                stQueueData &QueItem = OutQueue.back();

                char *pszData       = (char*) (QueItem.pData);
                Header.uiSession    = QueItem.uiSession;
                Header.DataType     = QueItem.DataType;

                OutQueue.pop_back();
                lockOutQue.unlock();
printf ("Msg Send. \n");
                Header.Op           = OP_RESPONSE;
                Header.u64Size      = (strlen(pszData) + 1) * sizeof(char);

                // write data

                memcpy ((__int8*)pSenderSwap + 1024, pszData, Header.u64Size);

                Header.flagLock     = SWAP_DATA_READY;

                /// @todo DO preform mem recycle

                free (pszData);
            }
        }
    }

    void ReceiverTheradFn ()
    {

        /// @todo
        /// @todo Needs a buffer (const char) to copy down string in shared mem.

        LPVOID              pRecvSwap   = (__int8*)pSwapMapped + (!flagClientSide ? 0 : _SWAP_MEM_SIZE);     // receiver (matcher side) reads from high half of shaed memory
        stSwapDataHeader   &Header      = *(stSwapDataHeader*)pRecvSwap;
        // Similar code chunk in constructor and SenderThreadFn()
        stQueueData         QueItem;

        while (this->flagRecvrAlive)
        {
std::this_thread::sleep_for(std::chrono::milliseconds(1));

            void *pBuffer       = malloc (_SWAP_MEM_SIZE - 1024);

            while(Header.flagLock != SWAP_DATA_READY);  // wait

            /// @todo change while() wait to mutex

printf ("Msg Recvd. \n");

            QueItem.DataType    = Header.DataType;
            QueItem.Op          = Header.Op;
            QueItem.uiSession   = Header.uiSession;
            memcpy (pBuffer, (__int8*)pRecvSwap + 1024, Header.u64Size * sizeof(char));

            Header.flagLock     = SWAP_WRITEABLE;       // Return swap mem to serverlet

            QueItem.pData       = pBuffer;

            InQueue.push_back (QueItem);
        }
    }

    /// @brief Fetch items in InQueue and parse, then schedule tasks into threadpool
    void TaskDistributerThreadFn ()
    {
        /// @todo add a assist funcion. 
        //        particularly for java interface callback. in other usecase, assist function could be empty.   
        //        conosider to defined it by a SENDER_TH_CALLBACK macro

        while (this->flagTskrAlive)
        {
std::this_thread::sleep_for(std::chrono::milliseconds(1));
            while (InQueue.empty() == true);   // wait
            
            /// @todo change while() wait to mutex

            lockInQue.lock();
            stQueueData QueItem = InQueue.back();
            if (QueItem.DataType == TYP_STR)
                printf ("   Content: %s\n", (char*)(QueItem.pData));                                  ////////////////////////////////////
            
            free (QueItem.pData);
            InQueue.pop_back();
            lockInQue.unlock();
        }
    }


public:

    ServerletCommunicator () : ServerletCommunicator (SIDE_A) { }
    ServerletCommunicator (bool flagSide)
    {
        this->flagClientSide = flagSide;
        
        // Init Shared Mem
        this->hMapFile = CreateFileMappingA(
            INVALID_HANDLE_VALUE,   // File hnd
            NULL,                   // Dflt security level
            PAGE_READWRITE,         // R&W
            0,                      // Hi size
            _SWAP_MEM_SIZE * 2,     // Lo size
            _SWAP_MEM_NAME          // Name
            );

        if (!hMapFile)
        {
            CloseHandle(hMapFile);
            return;
        }

        this->pSwapMapped = MapViewOfFile(
            hMapFile,               // File hnd
            FILE_MAP_ALL_ACCESS,    // R&W
            0,
            0,
            _SWAP_MEM_SIZE * 2      // 本例中，填写 BIG_BUF_SIZE 的效果等价于此处填 0
            );

        if (!pSwapMapped)
            return;
        

        /// @todo Init threads
        this->flagSenderAlive = true;
        this->flagRecvrAlive  = true;
        this->flagTskrAlive   = true;
        
        thSender    = std::thread(&ServerletCommunicator::SenderThreadFn,           this);
        thRecvr     = std::thread(&ServerletCommunicator::ReceiverTheradFn,         this);
        thTskr      = std::thread(&ServerletCommunicator::TaskDistributerThreadFn,  this);

        /// @todo init data in swap mem

        LPVOID              pSenderSwap = (__int8*)pSwapMapped + (flagClientSide ? 0 : _SWAP_MEM_SIZE);   // Sender (matcher side) write to low half of shaed memory
        stSwapDataHeader   &Header      = *(stSwapDataHeader*)pSenderSwap;
        // Similar code chunk in SenderThreadFn() and RecviverThreadFn()

        Header.DataType     = TYP_DEFAULT;
        Header.Op           = OP_NO_OP;
        Header.u64Size      = 0;
        Header.uiSession    = 0;
        Header.flagLock     = SWAP_DATA_READY;

    }

    ~ServerletCommunicator()
    {

        UnmapViewOfFile(pSwapMapped);
        CloseHandle(hMapFile);

        // Terminate threads
        flagSenderAlive = false;
        flagRecvrAlive  = false;
        flagTskrAlive   = false;

        thSender.join();
        thRecvr.join();
        thTskr.join();
    }

    bool isValidObj ()
    {
        if (!(this->hMapFile) || !(this->pSwapMapped))
            return false;
        
        return true;
    }
    
    /// @brief          Send data to serverlet (enqueue message). Responsible for processing data to send to raw string in swap
    /// @param pData    Data to sent, for now only String type allocated with keyword new is expected
    /// @param DType    Defined by TYP code 
    /// @param Op       Operaton requested, defined by OP code 
    /// @param u64Size  Data length in byte
    /// @param flagDel  Wether to delete pData. BE TO PASS ONLY AN OBJECT IF SET TO TRUE
    /// @return         Bytes send, or 0 if error encountered
    unsigned long long Send (void *pData, int DType, int Op, unsigned __int32 uiSession, bool flagDel)
    {
        if (!pData)
            return 0;

        /// @todo Do perform memory recycle
        /// @todo Convert data input to raw string data in swap mem, and warp it a stQueueData type

        /// @todo Add an element to OutQueue
        stQueueData QueItem;

        QueItem.DataType    = DType;
        QueItem.Op          = Op;
        QueItem.uiSession   = uiSession;
        QueItem.pData       = pData;

        lockOutQue.lock();
        this->OutQueue.push_back(QueItem);
        lockOutQue.unlock();

        if (flagDel)
            delete pData;
        
        return 0;
    }


    /// @brief End communication with serverlet. Called on terminatation of main thread.
    void close (void)
    {
        /// @todo Close shared memory handle here
        return;
    }
};
