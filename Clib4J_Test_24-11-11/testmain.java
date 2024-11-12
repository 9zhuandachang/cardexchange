import org.clib4j.communicator.*;
import org.clib4j.Matcher.*;

public class testmain 
{
    public static void main(String[] args) 
    {  
    
        //communicator.printnumber(561232);

        Matcher MatcherInstance = new Matcher();

        //String strXML   = Matcher.getDes("E:\\Pictures\\HMCatalog\\Outputs\\DSC00926.jpg", 0, MatcherInstance.C_Ptr);
        String strXML = "E:\\Pictures\\HMCatalog\\Outputs\\DSC00926.XML";
        Matcher.Append(strXML, 11, 0, MatcherInstance.C_Ptr); 

        int result[]    = Matcher.QueryByPic("E:\\Pictures\\NameHolder\\DSC00927.jpg", 5, 0, MatcherInstance.C_Ptr);

        System.out.println();
        System.out.println(strXML);
        if (result.length > 0)
            System.out.println(result[0]);
        else
            System.out.println("No result.");
    }
}
