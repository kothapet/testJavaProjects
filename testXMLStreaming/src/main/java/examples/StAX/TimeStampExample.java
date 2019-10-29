package examples.StAX;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class TimeStampExample {

    //private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

    public static void main(String[] args) {

		/*
        //method 1
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        System.out.println(timestamp);

        //method 2 - via Date
        Date date = new Date();
        System.out.println(new Timestamp(date.getTime()));

        //return number of milliseconds since January 1, 1970, 00:00:00 GMT
        System.out.println(timestamp.getTime());

        //format timestamp
        //System.out.println(sdf.format(timestamp));
		Path inputDir =  Paths.get("C:/EclipseNeonWorkSpace/OmniXML/omni_data/out/");
		String inputPattern = "omnilog20181212*.xml";
		*/

		try {
			/*
			DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, inputPattern);
		    for (Path entry: stream) {
		    	System.out.println("file : " + entry.toFile().getAbsolutePath() );
		    	System.out.println("file : " + entry.toFile().getName() );
		    }
			final String QUOTE = "\"";
			String test1 = "xyz" + QUOTE + "test"+ QUOTE + "junk";
	    	System.out.println("test1 : " + test1 );
	    	System.out.println("test1 : " + QUOTE+test1.replace(QUOTE, QUOTE+QUOTE)+QUOTE );
		    */

			TreeMap<String, String> testMap = new TreeMap<String, String>();
			testMap.put("001","val 1");
			testMap.put("000","Val 0");
			testMap.put("002","val 2");
			testMap.put("#00","key1");
			testMap.put("#01","key2");

			for(Map.Entry<String, String> entry : testMap.entrySet()) {
		    	System.out.println("key : " + entry.getKey() + "   VALUE: " + entry.getValue() );
			}

		} catch (Exception e) {
			e.printStackTrace();
		}


    }

}