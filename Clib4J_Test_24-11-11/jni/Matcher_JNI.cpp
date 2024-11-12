#include "org_clib4j_Matcher_Matcher.h"
#include "Communicator.cpp"
#include "Matcher.cpp"
#include <stdio.h>

#ifdef _HMDBG
#include <windows.h>
#endif

	/// @todo in communicator program add a callback to pass data into java serverlet

#define logFile "MatcherJNI.log"

cv::Mat GetMatFromXML (const char *szXMLFile)
{
    // 创建一个FileStorage对象并打开XML文件
    cv::FileStorage fs(szXMLFile, cv::FileStorage::READ);

    // 检查文件是否成功打开
    if (!fs.isOpened()) 
    {
        FILE *fp = fopen (logFile, "wt");
        if (fp)
            fprintf (fp, "Filed to open file: %s", szXMLFile);
        fclose (fp);

        return cv::Mat();
    }
    // 读取XML文件中的Mat对象
    cv::Mat mat;
    fs["matrix"] >> mat;
    fs.release();

    return mat;
}

/// @brief manual mem mgmt ineed
/// @param szStr 
/// @param szExt 
/// @return 
char *szChangeExtention (const char *szStr, const char *szExt)
{       //szStr should has enough size
    if (!szStr || !szExt)
        return NULL;
    if (strlen(szStr) < strlen(szExt))
        return NULL;

    char *szNew  = (char*)malloc( (strlen(szStr) + strlen(szExt)) * sizeof(char) + 1);
    strcpy (szNew, szStr);

    char *pszStr = (char*)szNew,
         *pszExt = (char*)szExt;

    pszStr += strlen(pszStr) - 1;

    for (int i = 0; i < strlen (szNew); i++)
    {
        if (*pszStr == '.')
        {
            strncpy (pszStr, pszExt, strlen(pszExt));
            break;
        }

        pszStr--;
    }

    return szNew;
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    C_Constructor
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_clib4j_Matcher_Matcher_C_1Constructor
  (JNIEnv *env, jclass cls)
{
    if (!SetDllDirectory(TEXT(".\\dll"))) 
    { 
        std::cerr << "Error setting DLL directory." << std::endl; 
        return 0; 
    }
#ifdef _HMDBG
    //MessageBoxA (NULL, "Matcher Constructor Called", "HM", MB_OK);
#endif
    return (jlong)(new Matcher);
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    C_Destructor
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_clib4j_Matcher_Matcher_C_1Destructor
  (JNIEnv *env, jclass cls, jlong C_Ptr)
{
    delete (Matcher*)C_Ptr;
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    Append
 * Signature: (Ljava/lang/String;II)V
 */
JNIEXPORT void JNICALL Java_org_clib4j_Matcher_Matcher_Append
  (JNIEnv *env, jclass cls, jstring DesXml, jint ID, jint iSession, jlong C_Ptr)
{
    const char *szDesXmlStr = env->GetStringUTFChars(DesXml, 0);
    
    cv::Mat mat = GetMatFromXML (szDesXmlStr);

    if (mat.empty())
        return;

    CardProp *prop = new CardProp(mat, (int)ID);
    ((Matcher*)C_Ptr)->Append (prop);
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    Query
 * Signature: (Ljava/lang/String;I)[B
 */
JNIEXPORT jintArray JNICALL Java_org_clib4j_Matcher_Matcher_Query
  (JNIEnv *env, jclass cls, jstring DesXml, jint iMaxReturn, jint iSession, jlong C_Ptr)
{
    const char *szDesXmlStr = env->GetStringUTFChars(DesXml, 0);
    
    cv::Mat mat = GetMatFromXML (szDesXmlStr);

    if (mat.empty())
        return nullptr;
    

   std::vector<card_id> IDList = ((Matcher*)C_Ptr)->Query (mat, iMaxReturn);

   jintArray outputArray = env->NewIntArray(IDList.size());
   env->SetIntArrayRegion(outputArray, 0, IDList.size(), (const jint*)IDList.data());

   return outputArray;
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    QueryByPic
 * Signature: (Ljava/lang/String;I)[I
 */
JNIEXPORT jintArray JNICALL Java_org_clib4j_Matcher_Matcher_QueryByPic
  (JNIEnv *env, jclass cls, jstring Path, jint iMaxReturn, jint iSession, jlong C_Ptr)
{
    const char *szPathStr = env->GetStringUTFChars(Path, 0);
    
   std::vector<card_id> IDList = ((Matcher*)C_Ptr)->Query (szPathStr, _DFLT_IMREAD_FLAG, iMaxReturn);

   jintArray outputArray = env->NewIntArray(IDList.size());
   env->SetIntArrayRegion(outputArray, 0, IDList.size(), (const jint*)IDList.data());

   return outputArray;
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    Remove
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_clib4j_Matcher_Matcher_Remove
  (JNIEnv *env, jclass cls, jint ID, jint iSession, jlong C_Ptr)
{
    return;
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    getDes
 * Signature: (Ljava/lang/String;I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_clib4j_Matcher_Matcher_getDes
  (JNIEnv *env, jclass cls, jstring Path, jint iSession, jlong C_Ptr)
{
    const char *szPathStr = env->GetStringUTFChars(Path, 0);
    
    CardProp card = ((Matcher*)C_Ptr)->CardProp_ByPath_DesOnly (szPathStr, _DFLT_IMREAD_FLAG, 0);

    if (card.validate() == false)
    {
        FILE *fp = fopen (logFile, "wt");
        if (fp)
            fprintf (fp, "Filed to open file: %s", szPathStr);
        fclose (fp);

        return nullptr;
    }

    char *szXMLPath = szChangeExtention (szPathStr, ".XML");

    cv::FileStorage fs(szXMLPath, cv::FileStorage::WRITE);
    if (fs.isOpened() == false)
    {
        FILE *fp = fopen (logFile, "wt");
        if (fp)
            fprintf (fp, "Filed to open file: %s", szXMLPath);
        fclose (fp);

        return nullptr;
    }
    fs << "matrix" << card.des;

    jstring outputStr = env->NewStringUTF (szXMLPath);  // copy char* string to a java string
    free (szXMLPath);

   return outputStr;
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    DesAndAppend
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT jstring JNICALL Java_org_clib4j_Matcher_Matcher_DesAndAppend
  (JNIEnv *env, jclass cls, jstring Path, jint ID, jint iSession, jlong C_Ptr)
{
    const char *szPathStr = env->GetStringUTFChars(Path, 0);
    
    CardProp prop = ((Matcher*)C_Ptr)->CardProp_ByPath_DesOnly (szPathStr, _DFLT_IMREAD_FLAG, (int)ID);

    if (prop.validate() == false)
    {
        FILE *fp = fopen (logFile, "wt");
        if (fp)
            fprintf (fp, "Filed to open file: %s", szPathStr);
        fclose (fp);

        return nullptr;
    }

    // Append card to candidate list
    ((Matcher*)C_Ptr)->Append (&prop);

    // Write des to XML
    char *szXMLPath = szChangeExtention (szPathStr, ".XML");

    cv::FileStorage fs(szXMLPath, cv::FileStorage::WRITE);
    if (fs.isOpened() == false)
    {
        FILE *fp = fopen (logFile, "wt");
        if (fp)
            fprintf (fp, "Filed to open file: %s", szXMLPath);
        fclose (fp);

        return nullptr;
    }
    fs << "matrix" << prop.des;

    jstring outputStr = env->NewStringUTF (szXMLPath);  // copy char* string to a java string
    free (szXMLPath);

   return outputStr;
}

/*
 * Class:     org_clib4j_Matcher_Matcher
 * Method:    Size
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_clib4j_Matcher_Matcher_Size
  (JNIEnv *env, jclass cls, jlong C_Ptr)
{
    return ((Matcher*)C_Ptr)->Size();
}
