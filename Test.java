package com.finicspro.processing.excel;

import static org.apache.poi.ss.formula.functions.Finance.*;
import java.util.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.poi.ss.formula.atp.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.*;

import com.mongodb.*;
import com.monitorjbl.xlsx.StreamingReader;
import java.io.*;
import java.nio.file.Paths;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.google.common.base.*;


public class Test {

    private static LocalDate toDate( String s_inputDt ) {
        String[] split = s_inputDt.split( "/" );

        java.util.Date inputDt = new java.util.Date( Integer.parseInt( split[2] ) - 1900, Integer.parseInt( split[1] ) - 1,
                                                     Integer.parseInt( split[0] ) );
        return inputDt.toInstant().atZone( ZoneId.systemDefault() ).toLocalDate();
    }

    public static void main( String[] als ) throws Exception {


        /*BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("d:\\temp\\a.csv")));
        String line = "";
        while((line = in.readLine()) != null) {
            StringTokenizer stz = new StringTokenizer(line, "\t");
            String l = stz.nextToken().trim();
            String dt = stz.nextToken().trim();

            StringTokenizer stz2 = new StringTokenizer(dt, "/");
            String m = stz2.nextToken().trim();
            String d = stz2.nextToken().trim();
            String y = stz2.nextToken().trim();

            String s = stz.nextToken().trim();

            System.out.println("db.performance.insert({\"LOAN_IDENTIFIER\":"+l+", \"MONTHLY_REPORTING_PERIOD\": new Date("+d+", "+m+", "+y+"), \"CURRENT_LOAN_DELINQUENCY_STATUS\":\""+s+"\"})");


        }
        in.close();*/

        /*String filePath = "D:\\VijayShare\\Version6\\Performance_2000Q2.txt";
        Path path = Paths.get(filePath);

        FileChannel fileChannel =  FileChannel.open(path);
        ByteBuffer buffer = ByteBuffer.allocate(6);
        int noOfBytesRead = fileChannel.read(buffer);
        int ttl = 0;

        PrintWriter out = new PrintWriter(new FileOutputStream("D:\\temp\\perf.psv"));
        while (noOfBytesRead != -1) {
            //System.out.println("Number of bytes read: " + noOfBytesRead);
            buffer.flip();
            //System.out.print("Buffer contents: ");

            while (buffer.hasRemaining()) {
                char c = (char) buffer.get();
                System.out.print(c);
                out.write( c );
            }

            //System.out.println(" ");
            buffer.clear();
            noOfBytesRead = fileChannel.read(buffer);

            ttl += noOfBytesRead;

            if(ttl >= 1024*1024) break;
        }
        fileChannel.close();
        out.close();*/

        long befT = System.currentTimeMillis();
        Map<String, ArrayList<PerfData>> map = new HashMap<String, ArrayList<PerfData>>( 1000000 );
        String buckets = "ABBCCCDDDEEE";

        Path path = FileSystems.getDefault().getPath( "D:\\VijayShare\\Version6\\", "Performance_2000Q2.txt" ); // reading Performance_2000Q2.txt took 127560 ms
        Files.lines( path ).forEach( line -> {
            List<String> lst = Splitter.on( '|' ).trimResults().splitToList( line );

            String loan = lst.get( 0 );
            String period = lst.get( 1 );
            int stts = 0;
            try {
                stts = Integer.parseInt( lst.get( 10 ) );
            } catch( NumberFormatException ex1 ) {
            }

            LocalDate tempLd = toDate( period );
            String s_bkt = "F";
            try {
                 s_bkt = buckets.charAt( stts )+"";
            } catch( Exception ex ) {
            }

            PerfData pd = new PerfData( loan, s_bkt, tempLd );

            if ( map.containsKey( loan ) ) {
                map.get( loan ).add( pd );
            } else {
                ArrayList<PerfData> al = new ArrayList<>();
                al.add( pd );
                map.put( loan, al );
            }
        } );

        int[][] observationMatrix = new int[8][8];

        for ( String loan : map.keySet() ) {

            ArrayList<PerfData> al = map.get( loan );
            Collections.sort( al );
            char prev, curr;
            if ( al.size() > 1 )
                for ( int i = 0, j = al.size() - 1; i < j; i++ ) {
                    prev = al.get( i ).stts.charAt( 0 );
                    curr = al.get( i + 1 ).stts.charAt( 0 );

                    try {
                        observationMatrix[( (char)prev ) - 65][( (char)curr ) - 65]++;
                    } catch( Exception | Error er ) {
                        //ex.printStackTrace();
                    }

                }
        }

        System.out.println("map.size: " + map.size());

        printMatrix( observationMatrix );

        long aftT = System.currentTimeMillis();

        System.out.println( ( aftT - befT ) + " ms" );
        if ( true )
            return;

        /*Comparator<Person> compareByName = Comparator.comparing( ( Person p ) -> p.firstName )
                                                 .thenComparingInt( p -> p.age );*/
        long bef = System.currentTimeMillis();

        //InputStream is = new FileInputStream( new File( "D:\\VijayShare\\Version5\\Performance.xlsx" ) );

        MongoClient mongo = new MongoClient( "localhost", 27017 );
        DB db = mongo.getDB( "Acadgild" );
        DBCollection table = db.getCollection( "performance" );

        /*StreamingReader reader = StreamingReader.builder()
                                                .rowCacheSize( 100 )    // number of rows to keep in memory (defaults to 10)
                                                .bufferSize( 4096 )     // buffer size to use when reading InputStream to file (defaults to 1024)
                                                .sheetIndex( 0 )        // index of sheet to use (defaults to 0)
                                                .read( is );            // InputStream or File for XLSX file (required)
                                                */

        InputStream is = new FileInputStream( new File( "D:\\temp\\Performance.xlsx" ) );
        Workbook workbook = StreamingReader.builder().open( is );
        Sheet sheet = workbook.getSheetAt( 0 );

        for ( Row r : sheet ) {
            try {
                Cell LOAN_IDENTIFIER = r.getCell( 0 );
                Cell MONTHLY_REPORTING_PERIOD = r.getCell( 1 );
                Cell CURRENT_LOAN_DELINQUENCY_STATUS = r.getCell( 10 );

                BasicDBObject document = new BasicDBObject();
                document.put( "LOAN_IDENTIFIER", LOAN_IDENTIFIER.getStringCellValue() );
                document.put( "MONTHLY_REPORTING_PERIOD", MONTHLY_REPORTING_PERIOD.getDateCellValue() );
                document.put( "CURRENT_LOAN_DELINQUENCY_STATUS", CURRENT_LOAN_DELINQUENCY_STATUS.getStringCellValue() );
                table.insert( document );
            } catch( Exception ex ) {
                ex.printStackTrace();
            }
        }

        workbook.close();
        long aft = System.currentTimeMillis();

        System.out.println( ( aft - bef ) + " ms." );

        /*long bef = System.currentTimeMillis();


        DBCollection table = db.getCollection( "performance" );

        DBCursor cursor = table.find();

        while ( cursor.hasNext() ) {
            System.out.println( cursor.next() );
        }




        InputStream is = new FileInputStream( new File( "D:\\VijayShare\\Version5\\Performance.xlsx" ) );

        StreamingReader reader = StreamingReader.builder()
                                                .rowCacheSize( 100 )    // number of rows to keep in memory (defaults to 10)
                                                .bufferSize( 1024 )     // buffer size to use when reading InputStream to file (defaults to 1024)
                                                .sheetIndex( 0 )        // index of sheet to use (defaults to 0)
                                                .read( is );            // InputStream or File for XLSX file (required)

        int rowPtr = 0;
        for ( Row row : reader ) {
            if(rowPtr ++ == 0) continue;
                try {
                    Long LOAN_IDENTIFIER = (long) (row.getCell( 0 ).getNumericCellValue());
                    Date MONTHLY_REPORTING_PERIOD = row.getCell( 1 ).getDateCellValue();
                    String CURRENT_LOAN_DELINQUENCY_STATUS = row.getCell( 10 ).getStringCellValue();

                    BasicDBObject document = new BasicDBObject();
                    document.put("LOAN_IDENTIFIER", LOAN_IDENTIFIER);
                    document.put("MONTHLY_REPORTING_PERIOD", MONTHLY_REPORTING_PERIOD);
                    document.put("CURRENT_LOAN_DELINQUENCY_STATUS", CURRENT_LOAN_DELINQUENCY_STATUS);
                    table.insert(document);

                } catch( Exception ex ) {
                    //ex.printStackTrace();
                }
        }

        mongo.close();
        reader.close();

        long aft = System.currentTimeMillis();

        System.out.println((aft - bef) + " ms.");*/
        /*Calendar cal = Calendar.getInstance();
        cal.set( 1900, 0, 1, 0, 0, 0 );
        cal.set( Calendar.MILLISECOND, 0 );
        System.out.println( new java.util.Date( cal.getTimeInMillis() ) );

        Calendar cal2 = Calendar.getInstance();
        cal2.set( Calendar.MILLISECOND, 0 );
        cal2.set( 2016, 5, 30, 0, 0, 0 );

        cal2.add( Calendar.MONTH, -1 );

        System.out.println( new java.util.Date( cal2.getTimeInMillis() ) );

        System.out.println( cal2.getTimeInMillis() - cal.getTimeInMillis() );

        long daysBetween = ChronoUnit.DAYS.between( cal.toInstant(), cal2.toInstant() ) + 2;

        System.out.println( daysBetween );

        System.out.println(EDate(getCal(2016, 5, 30), -1));

        Calendar cal3 = Calendar.getInstance();
        cal3.set( 2016, 1, 26, 0, 0, 0 );
        cal3.add( Calendar.DAY_OF_MONTH, -42426 );
        System.out.println( cal3 );

        System.out.println(pmt( 3/100d, 150, 1500000d ) );

        Date d1 = new Date(getCal(2012, 0, 1).getTimeInMillis());
        Date d2 = new Date(getCal(2012, 6, 30).getTimeInMillis());

        System.out.println("=>"+YearFracCalculator.calculate( DateUtil.getExcelDate(d1, false), DateUtil.getExcelDate(d2, false), 0 ));
        System.out.println("=>"+YearFracCalculator.calculate( DateUtil.getExcelDate(d1, false), DateUtil.getExcelDate(d2, false), 1 ));
        System.out.println("=>"+YearFracCalculator.calculate( DateUtil.getExcelDate(d1, false), DateUtil.getExcelDate(d2, false), 3 ));

        System.out.println("->" + Math.pow(4.0, 2));

        System.out.println("->>" + DateDiff(getCal(2019, 0, 1), getCal(2019, 1, 28), "d"));
        System.out.println("->>" + DateDiff(getCal(2019, 0, 1), getCal(2019, 1, 28), "m"));*/

    }

    private static long EDate( Calendar cal, int offset ) {
        Calendar epoch = getCal( 1900, 0, 1 );
        cal.add( Calendar.MONTH, offset );
        return ChronoUnit.DAYS.between( epoch.toInstant(), cal.toInstant() ) + 2; // why 2? to match excel result
    }

    private static long DateDiff( Calendar d1, Calendar d2, String unit ) {

        if ( "D".equalsIgnoreCase( unit ) ) {
            return Math.abs( ChronoUnit.DAYS.between( d1.toInstant(), d2.toInstant() ) ); // months not supported
        } else { // assume it is months difference
            int diffYear = d1.get( Calendar.YEAR ) - d2.get( Calendar.YEAR );
            int diffMonth = diffYear * 12 + d1.get( Calendar.MONTH ) - d2.get( Calendar.MONTH );
            return Math.abs( diffMonth );
        }
    }

    private static Calendar getCal( int yy, int mm, int dd ) {
        Calendar cal = Calendar.getInstance();
        cal.set( yy, mm, dd, 0, 0, 0 );
        cal.set( Calendar.MILLISECOND, 0 );
        return cal;
    }

    private void CASHFLOW( Calendar issueDate, Calendar maturityDate, double rate, double amount, int frequency, int dayCount,
                           boolean isPrincipalPayment ) {
        long count, startCol;
        double balance, totAmount;
        double[][] cf;
        int noOfMonths, periods;
        Calendar startDate, endDate;

        noOfMonths = 12 / frequency;
        periods = (int)DateDiff( issueDate, maturityDate, "m" ) / noOfMonths;
        //endDate = EDate(issueDate, periods * noOfMonths);
    }

    static void printMatrix( int[][] matrix ) {
        for ( int i = 0; i < matrix.length; i++ ) {
            for ( int j = 0; j < matrix[i].length; j++ ) {
                System.out.print( matrix[i][j] + "," );
            }
            System.out.println( "\n" );
        }
    }

/*
  static public double pmt( double r, int nper, double pv, double fv, int type ) {
double pmt = -r * ( pv * Math.pow( 1 + r, nper ) + fv ) / ( ( 1 + r * type ) * ( Math.pow( 1 + r, nper ) - 1 ) );
return pmt;
  }

  static public double pmt( double r, int nper, double pv, double fv ) {
return pmt( r, nper, pv, fv, 0 );
  }

  static public double pmt( double r, int nper, double pv ) {
return pmt( r, nper, pv, 0 );
  }

  static public double ipmt( double r, int per, int nper, double pv, double fv, int type ) {
double ipmt = fv( r, per - 1, pmt( r, nper, pv, fv, type ), pv, type ) * r;
if ( type == 1 )
  ipmt /= ( 1 + r );
return ipmt;
  }

  static public double ipmt( double r, int per, int nper, double pv, double fv ) {
return ipmt( r, per, nper, pv, fv, 0 );
  }

  static public double ipmt( double r, int per, int nper, double pv ) {
return ipmt( r, per, nper, pv, 0 );
  }

  static public double ppmt( double r, int per, int nper, double pv, double fv, int type ) {
return pmt( r, nper, pv, fv, type ) - ipmt( r, per, nper, pv, fv, type );
  }

  static public double ppmt( double r, int per, int nper, double pv, double fv ) {
return pmt( r, nper, pv, fv ) - ipmt( r, per, nper, pv, fv );
  }

  static public double ppmt( double r, int per, int nper, double pv ) {
return pmt( r, nper, pv ) - ipmt( r, per, nper, pv );
  }

  static public double fv(double r, int nper, double pmt, double pv, int type) {
double fv = -(pv * Math.pow(1 + r, nper) + pmt * (1+r*type) * (Math.pow(1 + r, nper) - 1) / r);
return fv;
}
static public double fv(double r, int nper, double c, double pv) {
  return fv(r, nper, c, pv, 0);
}*/
    //https://www.programcreek.com/java-api-examples/index.php?source_dir=GeneralPOI-master/src/org/apache/poi/hssf/record/formula/functions/FinanceLib.java
//  https://apache.googlesource.com/poi/+/4d81d34d5d566cb22f21999e653a5829cc678ed5/src/java/org/apache/poi/ss/formula/functions/Finance.java
//  https://jar-download.com/artifacts/org.apache.poi/poi/4.0.0/source-code/org/apache/poi/ss/formula/atp/YearFracCalculator.java
}
