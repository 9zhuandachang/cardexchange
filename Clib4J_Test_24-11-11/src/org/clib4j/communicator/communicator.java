package org.clib4j.communicator;

/**
 * javac communicator.java -d ..\..\..\..\out\
 * javac communicator.java -h ..\..\..\..\jni\
 */


public final class communicator
{
/* Operation codes, matcher to serverlet */ 
public static final int OP_NO_OP            = 0;           // Reserved
public static final int OP_REQ_INIT_DATA    = 1;           // Request data in DB from serverlet to initilize canditate list
public static final int OP_RESPONSE         = 2;           // Response to a query, one ID for each linew

// Operation codes, serverlet to matcher
public static final int OP_SVR_NO_OP        = 0 << 8;      // Reserved
public static final int OP_CRT_DES          = 1 << 8;      // Request to create descriptor for a picture.          With TYP_ID_W_PTH
public static final int OP_QUERY            = 2 << 8;      // Query a picture with it's descriptor or path.        With TYP_ID_W_DES(_EX) or TYP_ID_W_PTH(_EX)
public static final int OP_END_OF_DB_ITEM   = 3 << 8;      // The lase message send to matcher on init.            With TYP_DEFAULT
public static final int OP_NEW_ITEM         = 4 << 8;      // Add a new item to candidate list,                    With TYP_ID_W_DES(_EX) or TYP_ID_W_PTH(_EX)
public static final int OP_MTCHR_SHTDWN     = 254 << 8;    // Terminate matcher program                            With TYP_DEFAULT
public static final int OP_MTCHR_REST       = 255 << 8;    // Refresh matcher program state                        With TYP_DEFAULT

// Data types
public static final int TYP_DEFAULT         = 0;           // Reserved
public static final int TYP_ID_W_DES        = 1;           // ID - descriptor pair     Line1: ID, Rest: XML-parsed descriptor      With OP_QUERY
public static final int TYP_ID_W_PTH        = 2;           // ID - file path pair      Line1: ID, Line2: file path                 With OP_CRT_DES or OP_QUERY
public static final int TYP_ID_W_DES_EX     = 3;           // ID - descriptor pair     Line1: ID, Line2: max IDs in response, Rest: XML-parsed descriptor      With OP_QUERY
public static final int TYP_ID_W_PTH_EX     = 4;           // ID - file path pair      Line1: ID, Line2: max IDs in response, Line3: file path                 With OP_QUERY

public static final int TYP_IDS             = 5;           // List of ID, one for each line                                        With OP_RESPONSE

public static final int TYP_STR             = 1 << 8;      // Zero terminated string. For internal struture: data in stQueue


    static {
        System.load(System.getProperty("user.dir") + "\\dll\\test.dll"); // 这里这个 test 是你 dll 的名字
		//System.loadLibrary("HMCommunicator");
    }

	public static native void printnumber(int number);


	public static native void C_Constructor ();
	public static native void C_isValidObj (long lInstance);	// lInstance is pointer of C/C++ side instance converted to long
	public static native void C_Send (String stData, int DType, int Op, int iSession, boolean flagDel);

	/// @todo add a callback function to receive data

	/// @todo design a single send-receive function for single threaded processing (aka wait till value return)

	public communicator ()
	{
		
		return;
	}

	public boolean isValid ()
	{
		return false;
	}

	public void Send ()
	{

	}

}