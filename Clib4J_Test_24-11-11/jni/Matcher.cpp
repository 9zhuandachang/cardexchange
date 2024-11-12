#ifndef HM_SEPROJ_MATCHER
#define HM_SEPROJ_MATCHER

#include <opencv2/opencv.hpp>
#include <opencv2/xfeatures2d.hpp>
#include <vector>
#include <iostream>
#include <numeric>
#include <string>
#include <shared_mutex>
#include <exception>

#include "dirent.h"

#include "CardProp.cpp"


#define DFLT_IDS_RETURNED   4           // Number of IDs returned on maxium if not specified by query     

#define _MAX_FEATURES_SIFT  96
#define _DFLT_IMREAD_FLAG   cv::IMREAD_COLOR

#define _BLUR_RATIO         0.008
#define _BLUR_AMT           (int) (_MAX_PIC_HEIGHT * _BLUR_RATIO)
#define _MAX_PIC_HI_INT     800
//#define _MAX_PIC_HEIGHT     (argc>4 ? atoi(argv[4]) : _MAX_PIC_HI_INT)
#define _MAX_PIC_HEIGHT     _MAX_PIC_HI_INT

#define _AC_SCORE           125


class Matcher
{
private: 

    std::vector<std::string> ExtentionsList = {".jpg", ".png", ".jpeg", ".jfif"};

    /// @brief Cards to search thru. Contents provided by serverlet, IDs and des recorded in DB.
    std::vector<CardProp> CandidateList;

    cv::Ptr<cv::SIFT> sift;

    std::shared_mutex lockCndList;

    struct Score_ID_Pair
    {
        card_id id;
        float   score;
    };

    /// @brief utility for FilterAndListFolder
    bool compSuffix(const char *szStr, const char *szSuffix, bool flagCaseSensitive)
    {
        if (strlen(szSuffix) > strlen(szStr))
            return false;

        char *pszStr    = (char*)szStr,
            *pszSuffix = (char*)szSuffix;

        pszStr     += strlen (pszStr)    - 1;
        pszSuffix  += strlen (pszSuffix) - 1;

        if (flagCaseSensitive)
        {
            for (int i = strlen (pszSuffix); i; i--)
            {
                if (*pszStr != *pszSuffix)
                    return false;

                pszStr--;
                pszSuffix--;
            }
        }
        else
        {
            for (int i = strlen (szSuffix); i; i--)
            {
                if (tolower(*pszStr) != tolower(*pszSuffix))
                    return false;

                pszStr--;
                pszSuffix--;
            }
        }
        return true;
    }

    /// @brief Normalize a read picture, particularly for SIFT detection
    cv::Mat Nmlz (cv::Mat MatImg)
    {
        if (MatImg.empty())
            return cv::Mat();           // Empty return value
        
        if (cv::min(MatImg.cols, MatImg.rows) > _MAX_PIC_HEIGHT)
        {
            float fRatio = (float)_MAX_PIC_HEIGHT / cv::min(MatImg.cols, MatImg.rows);
            cv::resize (MatImg, MatImg, cv::Size (MatImg.cols * fRatio, MatImg.rows * fRatio), 0, 0, cv::INTER_NEAREST);
        }

        
        normalize (MatImg, MatImg, 1, 255, cv::NORM_MINMAX);
        
        blur (MatImg, MatImg, cv::Size(_BLUR_AMT, _BLUR_AMT));

        return MatImg;
    }

    /// @brief          Basic ReadAndNmlz function
    /// @param szPath   Full path to picture file
    /// @param flags    OpenCV ImgRead() flag 
    /// @return         Picture (cv::Mat)
    cv::Mat ReadAndNmlz (const char* szPath, int flags)
    {
        if (!szPath)
            return cv::Mat();

        cv::Mat MatImg = cv::imread(szPath, flags);
        if (MatImg.empty())
            return cv::Mat();           // Empty return value

        return Nmlz(MatImg);
    }
    /// @param szFolder Path to the folder that contains target picture file
    /// @param szName   Name of the picture file
    /// @param flags    OpenCV ImgRead() flag 
    /// @return         Picture (cv::Mat)
    cv::Mat ReadAndNmlz (const char *szFolder, const char *szName, int flags)
    {
        if (!szFolder || !szName)
            return cv::Mat();           // Empty return value

        char szPath[320];

        strcpy (szPath, szFolder);
        if (!compSuffix(szFolder, "\\", true))
            strcat (szPath, "\\");
        strcat (szPath, szName);

        return ReadAndNmlz (szPath, flags);
    }



public:
/*
    cv::Mat             Nmlz                    (cv::Mat);
    cv::Mat             ReadAndNmlz             (const char*, int);
    cv::Mat             ReadAndNmlz             (const char*, const char*, int);
    std::vector<std::string>  FilterAndListFolder     (const char*, unsigned char);
    float           DesCompare              (cv::Mat&, cv::Mat&);
    bool            compSuffix              (const char*, const char*, bool);
*/
    Matcher()
    {
        sift = cv::SIFT::create(_MAX_FEATURES_SIFT);
        return;
    }

    ~Matcher()
    {
        return;
    }

    /// @param szPath   Full path to picture file
    /// @param flags    OpenCV ImgRead() flag 
    /// @param id       id of card
    /// @return         CardProp structure, with NO descriptor
    CardProp ReadAndNmlz (const char* szPath, int flags, card_id id)
    {
        return CardProp(ReadAndNmlz(szPath, flags), cv::Mat(), id);
    }
    /// @param szFolder Path to the folder that contains target picture file
    /// @param szName   Name of the picture file
    /// @param flags    OpenCV ImgRead() flag 
    /// @param id       id of card
    /// @return         CardProp structure, with NO descriptor
    CardProp ReadAndNmlz (const char *szFolder, const char *szName, int flags, card_id id)
    {
        return CardProp(ReadAndNmlz(szFolder, szName, flags), cv::Mat(), id);
    }

    /// @brief          Basic function CardProp_ByPath
    /// @param szPath   Full path to picture file
    /// @param flags    OpenCV ImgRead() flag 
    /// @param id       id of card
    /// @return         CardProp structure
    CardProp CardProp_ByPath (const char* szPath, int flags, card_id id)
    {
        CardProp prop = ReadAndNmlz(szPath, flags, id);
        if (prop.validate() == false)
            return CardProp();
        std::vector<cv::KeyPoint> kpTmp;
        sift->detectAndCompute(prop.img, cv::noArray(), kpTmp, prop.des, false);
        return prop;
    }
    /// @param szFolder Path to the folder that contains target picture file
    /// @param szName   Name of the picture file
    /// @param flags    OpenCV ImgRead() flag 
    /// @param id       id of card
    /// @return         CardProp structure
    CardProp CardProp_ByPath (const char *szFolder, const char *szName, int flags, card_id id)
    {
        CardProp prop = ReadAndNmlz(szFolder, szName, flags, id);
        std::vector<cv::KeyPoint> kpTmp;
        sift->detectAndCompute(prop.img, cv::noArray(), kpTmp, prop.des, false);
        return prop;
    }
    /// @param szPath   Full path to picture file
    /// @param flags    OpenCV ImgRead() flag 
    /// @param id       id of card
    /// @return         CardProp structure, with NO img
    CardProp CardProp_ByPath_DesOnly (const char* szPath, int flags, card_id id)
    {
        CardProp prop = ReadAndNmlz(szPath, flags, id);
        std::vector<cv::KeyPoint> kpTmp;
        sift->detectAndCompute(prop.img, cv::noArray(), kpTmp, prop.des, false);
        prop.img.deallocate();
        return prop;
    }
    /// @param szFolder Path to the folder that contains target picture file
    /// @param szName   Name of the picture file
    /// @param flags    OpenCV ImgRead() flag 
    /// @param id       id of card
    /// @return         CardProp structure, with NO img
    CardProp CardProp_ByPath_DesOnly (const char *szFolder, const char *szName, int flags, card_id id)
    {
        CardProp prop = ReadAndNmlz(szFolder, szName, flags, id);
        std::vector<cv::KeyPoint> kpTmp;
        sift->detectAndCompute(prop.img, cv::noArray(), kpTmp, prop.des, false);
        prop.img.deallocate();
        return prop;
    }

    /// @param szPath       Path to folder
    /// @param chFolderNo   Reserved
    /// @return List of filtered file name, filtered by suffix. Filter defined by ExtentionsList{}
    std::vector<std::string> FilterAndListFolder (const char *szPath, unsigned char chFolderNo)
    {
        if (!szPath)
            return std::vector<std::string>();
        
        DIR             *dir    = opendir (szPath);
        struct dirent   *file   = NULL;
        std::vector<std::string>   FileList;
        if (!dir)
            return std::vector<std::string>();

        
        while (file = readdir(dir))
        {
            for (std::string strExt : ExtentionsList)
            {
                if (compSuffix(file->d_name, strExt.c_str(), false))
                {
                    FileList.push_back (file->d_name);
                    break;
                }
            }
        }

        closedir (dir);
        
        return FileList;   //return a node containing nothing in case an empty folder
    }
/*
    /// @brief For qsort() function. Asc. order for socre aka desc. order for similarity.
    bool compareScore (const void *A, const void *B)
    {
        return (*(Score_ID_Pair*)A).score < (*(Score_ID_Pair*)B).score;
    }
*/     
    /// @brief For qsort() function. Asc. order for socre aka desc. order for similarity.
    static bool compareScore (const Score_ID_Pair &A, const Score_ID_Pair &B)
    {
        return (A).score < (B).score;
    }

    /// @brief Compare the description std::vector of two pictures: Not tested
    /// @return Score
    float DesCompare (cv::Mat &matDes1, cv::Mat &matDes2)
    {
        if (matDes1.empty() || matDes2.empty())
            return INFINITY;

        float similarity_score;
        // for comparasion using BFMatcher
        cv::BFMatcher bf(cv::NORM_L2);
        std::vector<std::vector<cv::DMatch>> matches;
        // ratio test of matched descriptors
        std::vector<cv::DMatch> good_matches;
        // avg. distance between descriptors
        std::vector<float> distances;

        
        bf.knnMatch(matDes1, matDes2, matches, 2);

        for (const auto &m : matches)
            if (m[0].distance < 0.75 * m[1].distance)
                good_matches.push_back(m[0]);

        for (const auto &match : good_matches)
            distances.push_back(match.distance);

        if (!distances.empty())
            similarity_score = std::accumulate(distances.begin(), distances.end(), 0.0f) / distances.size();
        else
            similarity_score = std::numeric_limits<float>::infinity(); // 如果没有匹配项，设为无穷大
        
        return similarity_score;
    }


    /// @brief          Basic function of Query
    /// @param matDes   descriptor
    /// @param uiMaxResultsCnt max amount of items returned in std::vector<card_id>
    /// @return         best matching cards in card_id array (std::vector<card_id>)
    std::vector<card_id> Query (cv::Mat &matDes, const unsigned uiMaxResultsCnt)
    {
        if (matDes.empty() || !uiMaxResultsCnt)
            return std::vector<card_id>();

        Score_ID_Pair results[uiMaxResultsCnt];
        memset (&results, 0, sizeof(Score_ID_Pair) * uiMaxResultsCnt);
        for (unsigned ui = 0; ui < uiMaxResultsCnt; ui++)
            results[ui].score = INFINITY;

        unsigned    uiIndexOfHiScore    = uiMaxResultsCnt - 1;
        unsigned    uiResultsCnt        = 0;
        float       CndScore            = INFINITY;
        
        std::vector<card_id> results_ID_vec;


        for (CardProp Cnd : this->CandidateList)
        {
            std::shared_lock<std::shared_mutex> lock(lockCndList);
            CndScore = DesCompare (matDes, Cnd.des);

#ifdef _HM_DBG
printf ("Candidite: %d  Score: %3.3f\n", Cnd.id, CndScore);                                    ////////////////////////////////////////
#endif
            if (CndScore > _AC_SCORE)
                continue;       // skip if not a valid match

            /*
            Determine the index of candidate in results list that has the 
            highest score (lowest similarity) for now
            */
            float HiScore = 0;
            for (unsigned ui = 0; ui < uiMaxResultsCnt; ui++)
            {
                if (results[ui].score > HiScore)
                {
                    HiScore = results[ui].score;
                    uiIndexOfHiScore = ui;
                }
            }

            if (CndScore < HiScore)
            {
                results[uiIndexOfHiScore].score   = CndScore;
                results[uiIndexOfHiScore].id      = Cnd.id;

                if (uiResultsCnt < uiMaxResultsCnt)
                    uiResultsCnt += 1;
            }
        }

        std::sort (results, results + (uiMaxResultsCnt - 1), compareScore);
        for (unsigned ui = 0; ui < uiResultsCnt; ui++)
        {
            results_ID_vec.push_back(results[ui].id);
#ifdef _HM_DBG
printf ("Pushed into vec: %d\n", results[ui].id);
#endif
        }
        
        return results_ID_vec;
    }
    /// @brief          Query a picture
    /// @param matDes   descriptor
    /// @param uiMaxNum max amount of items returned in std::vector<card_id>
    /// @param result   best matching cards in card_id array (std::vector<card_id>)
    void Query (cv::Mat &matDes, const unsigned uiMaxNum, std::vector<card_id> &result)
    {  // params should be moded
        result = Query (matDes, uiMaxNum);
        return;
    }
    std::vector<card_id> Query (CardProp &card, const unsigned uiMaxNum)
    {
        return Query (card.des, uiMaxNum);
    }
    std::vector<card_id> Query (const char *szPath, int imreadFlags, const unsigned uiMaxNum)
    {
        CardProp card = CardProp_ByPath(szPath, imreadFlags, 0);
        return Query (card.des, uiMaxNum);
    }
    std::vector<card_id> Query (const char *szFolder, const char *szName, int imreadFlags, const unsigned uiMaxNum)
    {
        CardProp card = CardProp_ByPath(szFolder, szName, imreadFlags, 0);
        return Query (card.des, uiMaxNum);
    }
    /// @brief Functions in thread has no return value thus it needs to send the result somewhere itself
    void Query (CardProp &card, const unsigned uiMaxNum, std::vector<card_id> &result)  // params should be moded
    {
        result = Query (card, uiMaxNum);
        return;
    }
    void Query (const char *szPath, int imreadFlags, const unsigned uiMaxNum, std::vector<card_id> &result)
    {
        CardProp card = CardProp_ByPath(szPath, imreadFlags, 0);
        result = Query (card, uiMaxNum);
        return;
    }
    void Query (const char *szFolder, const char *szName, int imreadFlags, const unsigned uiMaxNum, std::vector<card_id> &result)
    {
        CardProp card = CardProp_ByPath(szFolder, szName, imreadFlags, 0);
        result = Query (card, uiMaxNum);
        return;
    }

    /// @brief          Basic function Append(). Add a card into candidate list
    /// @param  card    Card to be added. Must has a desription
    /// @return         Operation state, 0 for success
    int Append (CardProp *card)
    {
        if (!card || (*card).des.empty())
            return -1;
        
        std::unique_lock<std::shared_mutex> lock(lockCndList);

        // Possible to be optmized? As finding dublicate dosen't necessarily lock r&w
        // But would be a gap between unlock shared lock and then lock unique lock
        for (CardProp &cardCnd : this->CandidateList)
        {
            if (cardCnd.id == card->id)
            {
                (card->des).copyTo (cardCnd.des);   // Dublicate found, update des
                return 0;
            }
        }

        this->CandidateList.push_back (*card);

        return 0;
    }
    /// @brief              Add a card into candidate list
    /// @param  szPath      Path of picture of the card
    /// @param  imreadFlags Flag for imread() when reading the picture
    /// @return             Operation state, 0 for success
    int Append (const char* szPath, int imreadFlags, card_id id)
    {
        CardProp prop = CardProp_ByPath (szPath, imreadFlags, id);
        return Append (&prop);
    }
    /// @brief              Add a card into candidate list
    /// @param  szFolder    Path to the folder that contains target picture file
    /// @param  szName      Name of the picture file
    /// @param  imreadFlags Flag for imread() when reading the picture
    /// @return             Operation state, 0 for success
    int Append (const char *szFolder, const char *szName, int imreadFlags, card_id id)
    {
        CardProp prop = CardProp_ByPath (szFolder, szName, imreadFlags, id);
        return Append (&prop);
    }

    int Size ()
    {
        return this->CandidateList.size();
    }
};


#endif