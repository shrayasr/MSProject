package helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utilities {
	public static Date getDate() throws ParseException {

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = cal.getTime();
		currentDate = formatter.parse(formatter.format(currentDate));
		return currentDate;
	}

}
