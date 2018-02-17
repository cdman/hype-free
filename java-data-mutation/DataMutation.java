import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataMutation {

	public static void main(String[] args) throws Exception {
		final int threads = Runtime.getRuntime().availableProcessors() + 1;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		
		while (true) {
			final Date d = new Date(1286358789000L);
			d.setDate(13); d.setHours(12); d.setMinutes(42); d.setMonth(2); d.setSeconds(54);
//			d.getTime();
			
			for (int i = 0; i < 2*threads; ++i) {
				pool.submit(new Runnable() {
					public void run() {
						String strDate = d.toString();
						if (!"Sat Mar 13 12:42:54 EET 2010".equals(strDate)) {
							System.err.println(strDate);
						}
					}
				});
			}
		}
	}

}
