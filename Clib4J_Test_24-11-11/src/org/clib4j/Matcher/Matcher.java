package org.clib4j.Matcher;

// Note that only one instance is expected. Otherwise undefined.

public class Matcher {
    static {

		System.load(System.getProperty("user.dir") + "\\dll\\HM_CardMatcher.dll");
    }

    /// @biref the return value (long) is a converted C pointer
	public static native long C_Constructor ();
	public static native void C_Destructor (long C_Ptr);
	public long C_Ptr;

	public static native void 	Append        (String stDesXml, int ID, 		int iSession,	long C_Ptr);
	public static native String DesAndAppend  (String stPath, 	int ID,			int iSession,	long C_Ptr);    // generate a descriptor for a picture, then append 
	public static native int[]	Query         (String stDesXml, int iMaxReturn,	int iSession,	long C_Ptr);
    public static native int[]	QueryByPic    (String stPath, 	int iMaxReturn,	int iSession,	long C_Ptr);
	public static native void 	Remove        (int    ID,     					int iSession,	long C_Ptr);
	public static native String getDes        (String strPath, 					int iSession,	long C_Ptr);	// Get path to generated XML
	public static native int	Size		  (													long C_Ptr);	

	public Matcher ()
	{
		C_Ptr = C_Constructor();
	}

	public void deallocate ()
	{
		C_Destructor(C_Ptr);
	}
    
}