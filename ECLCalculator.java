package com.finicspro.processing.excel;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.monitorjbl.xlsx.StreamingReader;


public class ECLCalculator {

    //static double[][] d_transitionMatrix = new double[8][8];

    Map<Integer, ObservationMatrix> m_txMatrix = new HashMap<>( 500 );

    public final static String buckets = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static void main( String[] argLst ) throws Exception {
        long bef = System.currentTimeMillis();
        ECLCalculator eclCalc = new ECLCalculator();
        eclCalc.computeTransitionMatrices();
        long aft = System.currentTimeMillis();
        System.out.println( ( aft - bef ) + "ms" );
        eclCalc.secondProcess();

    }

    ECLCalculator() throws Exception {

    }

    void secondProcess() throws FileNotFoundException {

        double dblForeClosureLag = 6;
        double dblHairCut = 40;
        double dblScenario1 = 100;
        double dblScenario2 = 5;
        double dblScenario3 = 2;
        double dblWAvgScenario1 = 30;
        double dblWAvgScenario2 = 50;
        double dblWAvgScenario3 = 20;
        double strECL_Condition = 90;

        InputStream is = new FileInputStream( new File( "D:\\VijayShare\\LoanPortFolio.xlsx" ) );
        StreamingReader reader = StreamingReader.builder()
                                                .rowCacheSize( 3 )    // number of rows to keep in memory (defaults to 10)
                                                .bufferSize( 4096 )     // buffer size to use when reading InputStream to file (defaults to 1024)
                                                .sheetIndex( 0 )        // index of sheet to use (defaults to 0)
                                                .read( is );            // InputStream or File for XLSX file (required)

        Map<String, Integer> indexMap = new HashMap<>( 150 );
        int i = 0;
        int j = 0;
        for ( Row r : reader ) {
            if ( i++ == 0 ) {
                for ( Cell c : r ) {
                    indexMap.put( c.getStringCellValue(), j++ );
                    System.out.println();
                }
            }

            try {

                Double DEF_DPD = getNumericValue( r.getCell( indexMap.get( "DEF_DPD" ) ) );
                String BUCKET_NAME = getBucket( DEF_DPD );

                Double BAL_TENOR = getNumericValue( r.getCell( indexMap.get( "BAL_TENOR" ) ) );
                Double BAL_YEARS = ( BAL_TENOR == null ) ? 0 : ( BAL_TENOR <= 0 ? 0 : BAL_TENOR / 12 );

                //Double TRAN_MATRIX = null;
                //String MTX_ID = null;
                Double PRIN_OS = getNumericValue( r.getCell( indexMap.get( "PRIN_OS" ) ) );
                if ( PRIN_OS == null )
                    PRIN_OS = 0d;

                Double OD_PRIN = getNumericValue( r.getCell( indexMap.get( "OD_PRIN" ) ) );
                if ( OD_PRIN == null )
                    OD_PRIN = 0d;

                Double OD_INTEREST = getNumericValue( r.getCell( indexMap.get( "OD_INTEREST" ) ) );
                if ( OD_INTEREST == null )
                    OD_INTEREST = 0d;

                Double PV_LOAN = PRIN_OS + OD_PRIN + OD_INTEREST;

                Double PROP_VALUE = getNumericValue( r.getCell( indexMap.get( "PROP_VALUE" ) ) );
                if ( PROP_VALUE == null )
                    PROP_VALUE = 0d;
                Double ROI = getNumericValue( r.getCell( indexMap.get( "ROI" ) ) );
                if ( ROI == null )
                    ROI = 0d;

                Double PV_COLLATERAL = Math.pow( 1 + ( ROI / 100 ), -dblForeClosureLag ) * ( 1 - ( dblHairCut / 100 ) ) *
                                       PROP_VALUE;

            } catch( Exception ex ) {
                //ex.printStackTrace(); at every rowCacheSize'th count, just ignore
            }

        }

        reader.close();
        
        System.out.println( "done" );
    }

    private Double getNumericValue( Cell cell ) {
        try {
            if ( cell == null )
                return null;
            if ( CellType.BLANK == cell.getCellType() )
                return null;
            if ( CellType.NUMERIC == cell.getCellType() )
                return cell.getNumericCellValue();
        } catch( Exception ex ) {} catch( Error ex ) {}
        return null;
    }

    private String getBucket( Double DEF_DPD ) {
        if ( DEF_DPD == null )
            return "F";
        if ( DEF_DPD < 1 )
            return "A";
        if ( DEF_DPD >= 1 && DEF_DPD <= 90 )
            return "B";
        if ( DEF_DPD >= 91 && DEF_DPD <= 180 )
            return "C";
        if ( DEF_DPD >= 181 && DEF_DPD <= 270 )
            return "D";
        if ( DEF_DPD >= 271 && DEF_DPD <= 365 )
            return "E";
        return "F";

    }

    void computeTransitionMatrices() throws Exception {

        int[][] observationMatrix = new int[8][8];

        File myFile = new File( "D:/VijayShare/Output/DebtorsDPD.xlsx" );
        //File myFile = new File( "D:\\VijayShare\\DebtorsDPD_Original File.xlsx" );

        FileInputStream fis = new FileInputStream( myFile );

        XSSFWorkbook myWorkBook = new XSSFWorkbook( fis );
        XSSFSheet mySheet = myWorkBook.getSheetAt( 0 );
        Iterator<Row> rowIterator = mySheet.iterator();

        Row header = rowIterator.next();
        final int startIndex = getStartIndex( header, "Q22013" );
        final int endIndex = getEndIndex( header, "Q12018" );// header.getLastCellNum();   

        System.out.println( startIndex + " " + endIndex );

        while ( rowIterator.hasNext() ) {
            Row row = rowIterator.next();

            int t_startIndex = startIndex;
            int t_endIndex = endIndex;

            //Iterator<Cell> cellIterator = row.cellIterator();

            String borrowerId = getCellValue( row.getCell( 0 ) );
            System.out.println( borrowerId );

            while ( t_startIndex < t_endIndex ) {
                int prevSt = getStateIndex( t_startIndex, row );
                t_startIndex += 9;
                int currSt = getStateIndex( t_startIndex, row );

                System.out.println( "==+=>" + prevSt + " " + currSt + " " + ( ( prevSt % 9 ) - 2 ) + " " +
                                    ( ( currSt % 9 ) - 2 ) );

                try {
                    observationMatrix[( ( prevSt % 9 ) - 2 )][( ( currSt % 9 ) - 2 )]++;
                } catch( Exception ex ) {
                    //ex.printStackTrace();
                }
            }
        }

        myWorkBook.close();

        double[][] txMatrix = prepareTransitionMatrix( observationMatrix );
        m_txMatrix.put( 0, new ObservationMatrix( txMatrix ) );

        for ( int i = 1; i < 500; i++ ) {
            double[][] n_txMatrix = multiplyMatrices( txMatrix, m_txMatrix.get( i - 1 ).transitionMatrix );
            m_txMatrix.put( i, new ObservationMatrix( n_txMatrix ) );
        }

        printMatrix( observationMatrix );
        printMatrix( txMatrix );

        System.out.println( m_txMatrix );

        /*ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("d:/temp/myArr.ser"));
        oos.writeObject( m_txMatrix);
        oos.close();*/
    }

    int getStartIndex( Row header, String startingQuarter ) {
        if ( header == null )
            return 2;
        if ( startingQuarter == null )
            return 2;
        int colCount = header.getLastCellNum();
        startingQuarter += "_A";
        for ( int i = 0; i < colCount; i++ ) {
            Cell cell = header.getCell( i );
            if ( startingQuarter.equalsIgnoreCase( cell.getStringCellValue() ) )
                return i;
        }
        return 2;
    }

    int getEndIndex( Row header, String endingQuarter ) {
        if ( header == null )
            return 2;
        if ( endingQuarter == null )
            return 2;
        int colCount = header.getLastCellNum();
        endingQuarter += "_A";
        for ( int i = 0; i < colCount; i++ ) {
            Cell cell = header.getCell( i );
            if ( endingQuarter.equalsIgnoreCase( cell.getStringCellValue() ) )
                return i;
        }
        return colCount;
    }

    double[][] prepareTransitionMatrix( int[][] observationMatrix ) {
        double[][] d_transitionMatrix = new double[8][8];

        for ( int i = 0; i < 8; i++ ) {
            long total = 0;
            for ( int j = 0; j < 8; j++ ) {
                total += observationMatrix[i][j];
            }

            for ( int j = 0; j < 8; j++ ) {
                if ( total != 0 )
                    d_transitionMatrix[i][j] = observationMatrix[i][j] / (double)total;
            }
        }

        return d_transitionMatrix;
    }

    int getStateIndex( int startIndex, Row row ) {
        for ( int index = startIndex; index < startIndex + 7; index++ ) {
            if ( getNumericCellValue( row.getCell( index ) ) > 0 )
                return index;
        }
        return -1;
    }

    double getNumericCellValue( Cell cell ) {
        if ( cell != null && cell.getCellType() == CellType.NUMERIC )
            return cell.getNumericCellValue();
        else
            return -1;
    }

    String getCellValue( Cell cell ) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return new BigDecimal( cell.getNumericCellValue() + "" ).toPlainString();
            default:
                return "";
        }
    }

    void printMatrix( int[][] matrix ) {
        for ( int i = 0; i < matrix.length; i++ ) {
            for ( int j = 0; j < matrix[i].length; j++ ) {
                System.out.print( matrix[i][j] + "\t\t" );
            }
            System.out.println( "\n" );
        }
    }

    void printMatrix( double[][] matrix ) {
        for ( int i = 0; i < matrix.length; i++ ) {
            for ( int j = 0; j < matrix[i].length; j++ ) {
                System.out.print( matrix[i][j] + "\t\t" );
            }
            System.out.println( "\n" );
        }
    }

    public static double[][] multiplyMatrices( double[][] firstMatrix, double[][] secondMatrix ) {
        double[][] product = new double[8][8];
        for ( int i = 0; i < 8; i++ ) {
            for ( int j = 0; j < 8; j++ ) {
                for ( int k = 0; k < 8; k++ ) {
                    product[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
                }
            }
        }

        return product;
    }
}

/*  boolean isCellEmpty( final Cell cell ) {
if ( cell == null ) { // use row.getCell(x, Row.CREATE_NULL_AS_BLANK) to avoid null cells
  return true;
}

if ( cell.getCellType() == CellType.BLANK ) {
  return true;
}

if ( cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty() ) {
  return true;
}

return false;
  }
*/

/*void printMatrix() {
for(int i=0; i<8; i++) {
    for(int j=0; j<8; j++) {
        System.out.print(observationMatrix[i][j] + "\t\t");
    }
    System.out.println("\n");
}
System.out.println("-------------------");
for(int i=0; i<8; i++) {
    for(int j=0; j<8; j++) {
        System.out.print(d_transitionMatrix[i][j] + "\t\t");
    }
    System.out.println("\n");
}
}*/
