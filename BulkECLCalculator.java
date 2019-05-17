package com.finicspro.processing.excel;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import com.monitorjbl.xlsx.StreamingReader;

class ObservationMatrix implements Serializable {
    double[][] transitionMatrix = new double[8][8];

    ObservationMatrix( double[][] transitionMatrix ) {
        this.transitionMatrix = transitionMatrix;
    }

    public String toString() {
        return transitionMatrix[0][0] + "";
    }
}

public class BulkECLCalculator {

    //static double[][] d_transitionMatrix = new double[8][8];

    public final static String buckets = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public final int DEFAULT_TX_MTX = Integer.MIN_VALUE;

    int[][] observationMatrix = new int[8][8];
    LocalDate maxMonthlyReportingPeriod = null;

    public static void main( String[] argLst ) throws Exception {
        BulkECLCalculator eclCalc = new BulkECLCalculator();
        /*LocalDate a = eclCalc.toDate(new Date(2019-1900, 5, 25));
        LocalDate b = eclCalc.toDate(new Date(2018-1900, 2, 02));
        
        long diff = ChronoUnit.MONTHS.between(a, b);
        System.out.println(diff);
        if(true)return;*/

        /* int[][] I = new int[4][2];
        int k = 0;
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 2; j++ ) {
                I[i][j] = k++;
            }
        }
        
        printMatrix( I );
        
        int[] correspondingRow = I[3];
        for ( int j = 0; j < correspondingRow.length; j++ ) {
            System.out.println( correspondingRow[j] );
        }
        
        String BUCKET_NAME = "C";
        int rowIndex = (int)BUCKET_NAME.charAt( 0 ) - 65;
        System.out.println( rowIndex );
        if ( true )
            return;*/

        long bef = System.currentTimeMillis();

        Map<Integer, ObservationMatrix> map = eclCalc.computeTransitionMatrices();
        eclCalc.secondProcess( map );
        long aft = System.currentTimeMillis();
        System.out.println( "total time taken: " + ( aft - bef ) + "ms" );
    }

    BulkECLCalculator() throws Exception {

    }

    void secondProcess( Map<Integer, ObservationMatrix> m_txMatrix ) throws IOException {

        double dblForeClosureLag = 6;
        double dblHairCut = 45;
        double dblScenario1 = 100;
        double dblScenario2 = 5;
        double dblScenario3 = 2;
        double dblWAvgScenario1 = 30;
        double dblWAvgScenario2 = 50;
        double dblWAvgScenario3 = 20;
        String strECL_Condition = "> 90";

//        InputStream is = new FileInputStream( new File( "D:\\VijayShare\\SampleData\\OutputECL.xlsx" ) );
        InputStream is = new FileInputStream( new File( "D:\\VijayShare\\Version5\\Acquisition.xlsx" ) );
        StreamingReader reader = StreamingReader.builder()
                                                .rowCacheSize( 10 )    // number of rows to keep in memory (defaults to 10)
                                                .bufferSize( 4096 )     // buffer size to use when reading InputStream to file (defaults to 1024)
                                                .sheetIndex( 0 )        // index of sheet to use (defaults to 0)
                                                .read( is );            // InputStream or File for XLSX file (required)

        Map<String, Integer> indexMap = new HashMap<>( 150 );
        int i = 0;
        int j = 0;

        double PV_LOAN_Stage1 = 0d, PV_COLLATERAL_Stage1 = 0d, SCE_1_ECL_Stage1 = 0d, SCE_2_ECL_Stage1 = 0d,
                SCE_3_ECL_Stage1 = 0d, WEIGHTED_AVG_Stage1 = 0d;
        double PV_LOAN_Stage2 = 0d, PV_COLLATERAL_Stage2 = 0d, SCE_1_ECL_Stage2 = 0d, SCE_2_ECL_Stage2 = 0d,
                SCE_3_ECL_Stage2 = 0d, WEIGHTED_AVG_Stage2 = 0d;
        double PV_LOAN_Stage3 = 0d, PV_COLLATERAL_Stage3 = 0d, SCE_1_ECL_Stage3 = 0d, SCE_2_ECL_Stage3 = 0d,
                SCE_3_ECL_Stage3 = 0d, WEIGHTED_AVG_Stage3 = 0d;
        double PV_LOAN_Total = 0d, PV_COLLATERAL_Total = 0d, SCE_1_ECL_Total = 0d, SCE_2_ECL_Total = 0d, SCE_3_ECL_Total = 0d,
                WEIGHTED_AVG_Total = 0d;
        
        FileOutputStream fos = new FileOutputStream("d:\\temp\\response.csv");
        fos.write( "LOAN_IDENTIFIER ,PV_LOAN ,PROP_VALUE ,PV_COLLATERAL ,SCE_1_A ,SCE_1_B ,SCE_1_C ,SCE_1_D ,SCE_1_E ,SCE_1_F ,SCE_2_A ,SCE_2_B ,SCE_2_C ,SCE_2_D ,SCE_2_E ,SCE_2_F ,SCE_3_A ,SCE_3_B ,SCE_3_C ,SCE_3_D ,SCE_3_E ,SCE_3_F ,SCE_1_ECL ,SCE_2_ECL ,SCE_3_ECL\n".getBytes());

        for ( Row r : reader ) {
            if ( i++ == 0 ) {
                for ( Cell c : r ) {
                    indexMap.put( c.getStringCellValue(), j++ );
                }
                continue;
            }

            try {

                String BUCKET_NAME = getCellValue( r.getCell( indexMap.get( "DELIQUENT_STATUS" ) ) );//getBucket( DEF_DPD );
                
                double LOAN_IDENTIFIER = getNumericCellValue( r.getCell( indexMap.get( "LOAN_IDENTIFIER" ) ), 0d );

                double ORIGINAL_LOAN_TO_VALUE = getNumericCellValue( r.getCell( indexMap.get( "ORIGINAL_LOAN_TO_VALUE" ) ), 0d );
                double ORIGINAL_COMBINED_LOAN_TO_VALUE = getNumericCellValue( r.getCell( indexMap.get( "ORIGINAL_COMBINED_LOAN_TO_VALUE" ) ),
                                                                              0d );
                double ORIGINAL_UPB = getNumericCellValue( r.getCell( indexMap.get( "ORIGINAL_UPB" ) ), 0d );

                double PROP_VALUE = ( ORIGINAL_LOAN_TO_VALUE / ORIGINAL_COMBINED_LOAN_TO_VALUE ) * ORIGINAL_UPB;

                double ORIGINAL_LOAN_TERM = getNumericCellValue( r.getCell( indexMap.get( "ORIGINAL_LOAN_TERM" ) ), 0d );

                Date ORIGINATION_DATE = r.getCell( indexMap.get( "ORIGINATION_DATE" ) ).getDateCellValue();
                //int BAL_TENOR = (int)getNumericCellValue( r.getCell( indexMap.get( "BAL_TENOR" ) ), 0d );

                int BAL_TENOR = (int)( ORIGINAL_LOAN_TERM - Math.abs( ChronoUnit.MONTHS.between( maxMonthlyReportingPeriod,
                                                                                                 toDate( ORIGINATION_DATE ) ) ) );

                int BAL_YEARS = ( BAL_TENOR <= 0 ) ? 0 : BAL_TENOR / 12;
                int TRAN_MATRIX = 0;

                if ( "All".equals( strECL_Condition ) ) {
                    TRAN_MATRIX = DEFAULT_TX_MTX; // this is considered as DEFAULT
                } else if ( "> 0".equals( strECL_Condition ) ) {
                    if ( !"A".equals( BUCKET_NAME ) )
                        TRAN_MATRIX = DEFAULT_TX_MTX; // this is considered as DEFAULT
                } else if ( "> 90".equals( strECL_Condition ) ) {
                    if ( !"A".equals( BUCKET_NAME ) && !"B".equals( BUCKET_NAME ) )
                        TRAN_MATRIX = DEFAULT_TX_MTX; // this is considered as DEFAULT
                } else if ( "> 180".equals( strECL_Condition ) ) {
                    if ( !"A".equals( BUCKET_NAME ) && !"B".equals( BUCKET_NAME ) && !"C".equals( BUCKET_NAME ) )
                        TRAN_MATRIX = DEFAULT_TX_MTX; // this is considered as DEFAULT
                } else if ( "> 270".equals( strECL_Condition ) ) {
                    if ( !"A".equals( BUCKET_NAME ) && !"B".equals( BUCKET_NAME ) && !"C".equals( BUCKET_NAME ) &&
                         !"D".equals( BUCKET_NAME ) )
                        TRAN_MATRIX = DEFAULT_TX_MTX; // this is considered as DEFAULT
                } else if ( "> 365".equals( strECL_Condition ) ) {
                    if ( "F".equals( BUCKET_NAME ) )
                        TRAN_MATRIX = DEFAULT_TX_MTX; // this is considered as DEFAULT
                }

                if ( TRAN_MATRIX != DEFAULT_TX_MTX )
                    TRAN_MATRIX = ( BAL_TENOR == 0 ) ?
                                                      DEFAULT_TX_MTX :
                                                     ( "A".equals( BUCKET_NAME ) ? 1 : ( BAL_YEARS == 0 ) ? 1 : BAL_YEARS );


                /*if ( DEF_DPD > 144 ) { // if DEF_DPD > 12 years, default TRAN_MATRIX to DEFAULT_TX_MTX
                    TRAN_MATRIX = DEFAULT_TX_MTX; // this is considered as DEFAULT
                } else if ( DEF_DPD > getECLCondition( strECL_Condition ) || BAL_TENOR == 0 ) {
                    TRAN_MATRIX = 0;
                } else if ( DEF_DPD <= 30 ) {
                    TRAN_MATRIX = 1;
                } else if ( BAL_YEARS == 0 ) {
                    TRAN_MATRIX = 1;
                } else {
                    TRAN_MATRIX = BAL_YEARS;
                }*/

                String MTX_ID = TRAN_MATRIX + BUCKET_NAME;
                              
                double TM_A = 0d;
                double TM_B = 0d;
                double TM_C = 0d;
                double TM_D = 0d;
                double TM_E = 0d;
                double TM_F = 1d;

                if ( TRAN_MATRIX != DEFAULT_TX_MTX ) {
                    ObservationMatrix om = m_txMatrix.get( TRAN_MATRIX * 4 - 1 ); // 4 because each year has 4 quarters, - 1 because index starts with 0
                    int rowIndex = (int) BUCKET_NAME.charAt( 0 ) - 65; // (int) 'A' = 65
                    double[] correspondingRow = om.transitionMatrix[rowIndex];
                    // A == 65
                    TM_A = correspondingRow[0];
                    TM_B = correspondingRow[1];
                    TM_C = correspondingRow[2];
                    TM_D = correspondingRow[3];
                    TM_E = correspondingRow[4];
                    TM_F = correspondingRow[5];
                }
                
/*
                double PRIN_OS = getNumericCellValue( r.getCell( indexMap.get( "PRIN_OS" ) ), 0d );
                double OD_INTEREST = getNumericCellValue( r.getCell( indexMap.get( "OD_INTEREST" ) ), 0d );
*/
                double PV_LOAN = getNumericCellValue( r.getCell( indexMap.get( "CURRENT_UPB" ) ), 0d );

                double ORIGINAL_INTEREST_RATE = getNumericCellValue( r.getCell( indexMap.get( "ORIGINAL_INTEREST_RATE" ) ), 0d );
                //double PROP_VALUE = getNumericCellValue( r.getCell( indexMap.get( "PROP_VALUE" ) ), 0d );
                double PV_COLLATERAL = Math.pow( 1 + ( ORIGINAL_INTEREST_RATE / 100 ), -dblForeClosureLag ) * ( 1 - ( dblHairCut / 100 ) ) *
                                       PROP_VALUE; // correct until here
                

                double SCE_1_A = PV_LOAN * TM_A;
                double SCE_1_B = PV_LOAN * TM_B;
                double SCE_1_C = PV_LOAN * TM_C;
                double SCE_1_D = PV_LOAN * TM_D;
                double SCE_1_E = PV_LOAN * TM_E;
                double SCE_1_F = PV_LOAN * TM_F;

                double SCE_2_B = ( SCE_1_A == 0 ) ? SCE_1_B : SCE_1_B * ( 1 + ( dblScenario2 / 100 ) );
                double SCE_2_C = ( SCE_1_A == 0 ) ? SCE_1_C : SCE_1_C * ( 1 + ( dblScenario2 / 100 ) );
                double SCE_2_D = ( SCE_1_A == 0 ) ? SCE_1_D : SCE_1_D * ( 1 + ( dblScenario2 / 100 ) );
                double SCE_2_E = ( SCE_1_A == 0 ) ? SCE_1_E : SCE_1_E * ( 1 + ( dblScenario2 / 100 ) );
                double SCE_2_F = ( SCE_1_A == 0 ) ? SCE_1_F : SCE_1_F * ( 1 + ( dblScenario2 / 100 ) );

                double SCE_2_A = ( SCE_1_A == 0 ) ?
                                                  0 :
                                                  ( SCE_1_A + SCE_1_B + SCE_1_C + SCE_1_D + SCE_1_E + SCE_1_F ) -
                                                      ( SCE_2_B + SCE_2_C + SCE_2_D + SCE_2_E + SCE_2_F );

                double SCE_3_B = ( SCE_1_A == 0 ) ? SCE_1_B : SCE_1_B * ( 1 - ( dblScenario3 / 100 ) );
                double SCE_3_C = ( SCE_1_A == 0 ) ? SCE_1_C : SCE_1_C * ( 1 - ( dblScenario3 / 100 ) );
                double SCE_3_D = ( SCE_1_A == 0 ) ? SCE_1_D : SCE_1_D * ( 1 - ( dblScenario3 / 100 ) );
                double SCE_3_E = ( SCE_1_A == 0 ) ? SCE_1_E : SCE_1_E * ( 1 - ( dblScenario3 / 100 ) );
                double SCE_3_F = ( SCE_1_A == 0 ) ? SCE_1_F : SCE_1_F * ( 1 - ( dblScenario3 / 100 ) );

                double SCE_3_A = ( SCE_1_A == 0 ) ?
                                                  0 :
                                                  ( SCE_1_A + SCE_1_B + SCE_1_C + SCE_1_D + SCE_1_E + SCE_1_F ) -
                                                      ( SCE_3_B + SCE_3_C + SCE_3_D + SCE_3_E + SCE_3_F );

                double SCE_1_TEMP = 0d;
                double SCE_2_TEMP = 0d;
                double SCE_3_TEMP = 0d;

                if ( "All".equals( strECL_Condition ) ) {
                    SCE_1_TEMP = SCE_1_A + SCE_1_B + SCE_1_C + SCE_1_D + SCE_1_E + SCE_1_F - PV_COLLATERAL;
                    SCE_2_TEMP = SCE_2_A + SCE_2_B + SCE_2_C + SCE_2_D + SCE_2_E + SCE_2_F - PV_COLLATERAL;
                    SCE_3_TEMP = SCE_3_A + SCE_3_B + SCE_3_C + SCE_3_D + SCE_3_E + SCE_3_F - PV_COLLATERAL;
                } else if ( "> 0".equals( strECL_Condition ) ) {
                    SCE_1_TEMP = SCE_1_B + SCE_1_C + SCE_1_D + SCE_1_E + SCE_1_F - PV_COLLATERAL;
                    SCE_2_TEMP = SCE_2_B + SCE_2_C + SCE_2_D + SCE_2_E + SCE_2_F - PV_COLLATERAL;
                    SCE_3_TEMP = SCE_3_B + SCE_3_C + SCE_3_D + SCE_3_E + SCE_3_F - PV_COLLATERAL;

                } else if ( "> 90".equals( strECL_Condition ) ) {
                    SCE_1_TEMP = SCE_1_C + SCE_1_D + SCE_1_E + SCE_1_F - PV_COLLATERAL;
                    SCE_2_TEMP = SCE_2_C + SCE_2_D + SCE_2_E + SCE_2_F - PV_COLLATERAL;
                    SCE_3_TEMP = SCE_3_C + SCE_3_D + SCE_3_E + SCE_3_F - PV_COLLATERAL;

                } else if ( "> 180".equals( strECL_Condition ) ) {
                    SCE_1_TEMP = SCE_1_D + SCE_1_E + SCE_1_F - PV_COLLATERAL;
                    SCE_2_TEMP = SCE_2_D + SCE_2_E + SCE_2_F - PV_COLLATERAL;
                    SCE_3_TEMP = SCE_3_D + SCE_3_E + SCE_3_F - PV_COLLATERAL;
                    
                } else if ( "> 270".equals( strECL_Condition ) ) {
                    SCE_1_TEMP = SCE_1_E + SCE_1_F - PV_COLLATERAL;
                    SCE_2_TEMP = SCE_2_E + SCE_2_F - PV_COLLATERAL;
                    SCE_3_TEMP = SCE_3_E + SCE_3_F - PV_COLLATERAL;
                    
                } else if ( "> 365".equals( strECL_Condition ) ) {
                    SCE_1_TEMP = SCE_1_F - PV_COLLATERAL;
                    SCE_2_TEMP = SCE_2_F - PV_COLLATERAL;
                    SCE_3_TEMP = SCE_3_F - PV_COLLATERAL;
                }

                int SCE_1_ECL = ( SCE_1_TEMP < 0 ) ? 0 : (int)SCE_1_TEMP;
                int SCE_2_ECL = ( SCE_2_TEMP < 0 ) ? 0 : (int)SCE_2_TEMP;
                int SCE_3_ECL = ( SCE_3_TEMP < 0 ) ? 0 : (int)SCE_3_TEMP;

                double WEIGHTED_AVG = ( SCE_1_ECL * ( dblWAvgScenario1 / 100 ) ) + ( SCE_2_ECL * ( dblWAvgScenario2 / 100 ) ) +
                                      ( SCE_3_ECL * ( dblWAvgScenario3 / 100 ) );
                if ( "A".equals( BUCKET_NAME ) ) {
                    PV_LOAN_Stage1 += PV_LOAN;
                    PV_COLLATERAL_Stage1 += PV_COLLATERAL;
                    SCE_1_ECL_Stage1 += SCE_1_ECL;
                    SCE_2_ECL_Stage1 += SCE_2_ECL;
                    SCE_3_ECL_Stage1 += SCE_3_ECL;
                    WEIGHTED_AVG_Stage1 += WEIGHTED_AVG;
                } else if ( "B".equals( BUCKET_NAME ) ) {
                    PV_LOAN_Stage2 += PV_LOAN;
                    PV_COLLATERAL_Stage2 += PV_COLLATERAL;
                    SCE_1_ECL_Stage2 += SCE_1_ECL;
                    SCE_2_ECL_Stage2 += SCE_2_ECL;
                    SCE_3_ECL_Stage2 += SCE_3_ECL;
                    WEIGHTED_AVG_Stage2 += WEIGHTED_AVG;
                } else {
                    PV_LOAN_Stage3 += PV_LOAN;
                    PV_COLLATERAL_Stage3 += PV_COLLATERAL;
                    SCE_1_ECL_Stage3 += SCE_1_ECL;
                    SCE_2_ECL_Stage3 += SCE_2_ECL;
                    SCE_3_ECL_Stage3 += SCE_3_ECL;
                    WEIGHTED_AVG_Stage3 += WEIGHTED_AVG;
                }
                
                fos.write( (LOAN_IDENTIFIER + ", " + PV_LOAN + ", " + PROP_VALUE + ", " + PV_COLLATERAL + ", " +SCE_1_A + ", " + SCE_1_B + ", " + SCE_1_C + ", " + SCE_1_D + ", " + SCE_1_E + ", " + SCE_1_F + ", " + SCE_2_A + ", " + SCE_2_B + ", " + SCE_2_C + ", " + SCE_2_D + ", " + SCE_2_E + ", " + SCE_2_F + ", " + SCE_3_A + ", " + SCE_3_B + ", " + SCE_3_C + ", " + SCE_3_D + ", " + SCE_3_E + ", " + SCE_3_F + ", " + SCE_1_ECL + ", " + SCE_2_ECL + ", " + SCE_3_ECL + "\n").getBytes() );
                
                //System.out.println(r.getCell( 0 ).getStringCellValue() + " DEF_DPD:" + DEF_DPD  + " BUCKET_NAME:"+BUCKET_NAME + " BAL_YEARS:"+BAL_YEARS + " TRAN_MATRIX:"+TRAN_MATRIX);
                //System.out.println(r.getCell( 0 ).getStringCellValue() + "," + SCE_1_A + "," + SCE_1_B + "," + SCE_1_C + "," + SCE_1_D + "," + SCE_1_E + "," + SCE_1_F);
                //System.out.println(SCE_1_ECL + " " + SCE_2_ECL + " " + SCE_3_ECL);

            } catch( NullPointerException npex ) {

            } catch( Exception ex ) {
                ex.printStackTrace(); //at every rowCacheSize'th count, just ignore
            }

        }
        
        fos.close();

        PV_LOAN_Total = PV_LOAN_Stage1 + PV_LOAN_Stage2 + PV_LOAN_Stage3;
        PV_COLLATERAL_Total = PV_COLLATERAL_Stage1 + PV_COLLATERAL_Stage2 + PV_COLLATERAL_Stage3;
        SCE_1_ECL_Total = SCE_1_ECL_Stage1 + SCE_1_ECL_Stage2 + SCE_1_ECL_Stage3;
        SCE_2_ECL_Total = SCE_2_ECL_Stage1 + SCE_2_ECL_Stage2 + SCE_2_ECL_Stage3;
        SCE_3_ECL_Total = SCE_3_ECL_Stage1 + SCE_3_ECL_Stage2 + SCE_3_ECL_Stage3;
        WEIGHTED_AVG_Total = WEIGHTED_AVG_Stage1 + WEIGHTED_AVG_Stage2 + WEIGHTED_AVG_Stage3;

        NumberFormat formatter = new DecimalFormat( "#0.00" );
        System.out.println( formatter.format( PV_LOAN_Stage1 ) + "," + formatter.format( PV_COLLATERAL_Stage1 ) + "," +
                            formatter.format( SCE_1_ECL_Stage1 ) + "," + formatter.format( SCE_2_ECL_Stage1 ) + "," +
                            formatter.format( SCE_3_ECL_Stage1 ) + "," + formatter.format( WEIGHTED_AVG_Stage1 ) );
        System.out.println( formatter.format( PV_LOAN_Stage2 ) + "," + formatter.format( PV_COLLATERAL_Stage2 ) + "," +
                            formatter.format( SCE_1_ECL_Stage2 ) + "," + formatter.format( SCE_2_ECL_Stage2 ) + "," +
                            formatter.format( SCE_3_ECL_Stage2 ) + "," + formatter.format( WEIGHTED_AVG_Stage2 ) );
        System.out.println( formatter.format( PV_LOAN_Stage3 ) + "," + formatter.format( PV_COLLATERAL_Stage3 ) + "," +
                            formatter.format( SCE_1_ECL_Stage3 ) + "," + formatter.format( SCE_2_ECL_Stage3 ) + "," +
                            formatter.format( SCE_3_ECL_Stage3 ) + "," + formatter.format( WEIGHTED_AVG_Stage3 ) );
        System.out.println( formatter.format( PV_LOAN_Total ) + "," + formatter.format( PV_COLLATERAL_Total ) + "," +
                            formatter.format( SCE_1_ECL_Total ) + "," + formatter.format( SCE_2_ECL_Total ) + "," +
                            formatter.format( SCE_3_ECL_Total ) + "," + formatter.format( WEIGHTED_AVG_Total ) );

        reader.close();

        System.out.println( "done" );
    }

    private int getECLCondition( String strECL_Condition ) {
        if ( strECL_Condition == null )
            throw new IllegalArgumentException( "strECL_Condition is null" );
        else if ( strECL_Condition.equals( "> 0" ) )
            return 0;
        else if ( strECL_Condition.equals( "> 90" ) )
            return 90;
        else if ( strECL_Condition.equals( "> 180" ) )
            return 180;
        else if ( strECL_Condition.equals( "> 207" ) )
            return 270;
        else if ( strECL_Condition.equals( "> 365" ) )
            return 365;
        return -1;
    }

/*    private double getNumericValue( Cell cell ) {
try {
return cell.getNumericCellValue();

} catch( Exception ex ) {} catch( Error ex ) {}
return null;
}*/

/*    private String getBucket( double DEF_DPD ) {
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
*/
    Map<Integer, ObservationMatrix> computeTransitionMatrices() throws Exception {

        /* if ( true ) {
            ObjectInputStream ois = new ObjectInputStream( new FileInputStream( "d:/temp/myArr.ser" ) );
            Map<Integer, ObservationMatrix> map = (Map<Integer, ObservationMatrix>)ois.readObject();
            ois.close();
            return map;
        }*/

        observationMatrix = new int[8][8];
        maxMonthlyReportingPeriod = null;

        //File myFile = new File( "D:/VijayShare/Output/DebtorsDPD.xlsx" );
        //File myFile = new File( "D:\\VijayShare\\DebtorsDPD_Original File.xlsx" );

        //InputStream is = new FileInputStream( new File( "D:\\VijayShare\\DebtorsDPD_Original File.xlsx" ) );
        //InputStream is = new FileInputStream( new File( "D:\\VijayShare\\SampleData\\DebtorsDPD.xlsx" ) );
        //InputStream is = new FileInputStream( new File( "D:\\Sunil\\codes\\ECLFinancialModel\\Performance.xlsx" ) );
        InputStream is = new FileInputStream( new File( "D:\\VijayShare\\Version5\\Performance.xlsx" ) );

        StreamingReader reader = StreamingReader.builder()
                                                .rowCacheSize( 100 )    // number of rows to keep in memory (defaults to 10)
                                                .bufferSize( 1024 )     // buffer size to use when reading InputStream to file (defaults to 1024)
                                                .sheetIndex( 0 )        // index of sheet to use (defaults to 0)
                                                .read( is );            // InputStream or File for XLSX file (required)

        /*Map<String, Integer> indexMap = new HashMap<>( 150 );
        
        int rPtr = 0;
        int startIndex = 0;
        int endIndex = 0;
        for ( Row r : reader ) {
            if ( rPtr++ == 0 ) {
        
                startIndex = getStartIndex( r, "Q22013" );
                endIndex = getEndIndex( r, "Q12019" );// header.getLastCellNum();   
        
                System.out.println( "start Index and end index "+ startIndex + " " + endIndex );
                continue;
            }
            int t_startIndex = startIndex;
            int t_endIndex = endIndex;
        
            String borrowerId = getCellValue( r.getCell( 0 ) );
            //System.out.println( borrowerId );
            double outStandingAmt = 0d;    
            while ( t_startIndex < t_endIndex ) {
                outStandingAmt = getNumericCellValue(r.getCell( t_startIndex + 8 ), Double.MIN_VALUE);
                //System.out.println(">>>>>>"+outStandingAmt);
                //printMatrix(observationMatrix);
                int prevSt = getStateIndex( t_startIndex, r );
                t_startIndex += 9;
                int currSt = getStateIndex( t_startIndex, r );
        
                //System.out.println( "==+=>" + prevSt + " " + currSt + " " + ( ( prevSt % 9 ) - 2 ) + " " + ( ( currSt % 9 ) - 2 ) );
                
        
                try {
                    if(prevSt != -1 && currSt != -1 && outStandingAmt != D  ouble.MIN_VALUE && outStandingAmt > 0)
                        observationMatrix[( ( prevSt % 9 ) - 2 )][( ( currSt % 9 ) - 2 )] ++;
                } catch( Exception | Error er ) {
                    //ex.printStackTrace();
                }
            }
        }
        */

        Map<String, ArrayList<PerfData>> map = new HashMap<String, ArrayList<PerfData>>();
        int rPtr = 0;

        for ( Row r : reader ) {

            if ( rPtr++ == 0 ) { // ignore the header
                continue;
            }

            try {
                String loan = getCellValue( r.getCell( 0 ) );
                Date period = getDateValue( r.getCell( 1 ) );
                String stts = getCellValue( r.getCell( 10 ) );
                //System.out.println( loan + " " + period + " " + stts );

                LocalDate tempLd = toDate( period );

                if ( maxMonthlyReportingPeriod == null || tempLd.isAfter( maxMonthlyReportingPeriod ) ) {
                    maxMonthlyReportingPeriod = tempLd;
                }

                PerfData pd = new PerfData( loan, stts, tempLd );

                if ( map.containsKey( loan ) ) {
                    map.get( loan ).add( pd );
                } else {
                    ArrayList<PerfData> al = new ArrayList<>();
                    al.add( pd );
                    map.put( loan, al );
                }
            } catch( Exception ex ) {
                //ex.printStackTrace();
            }
        }

        System.out.println( "rPtr=" + rPtr );
        System.out.println( "maxMonthlyReportingPeriod=" + maxMonthlyReportingPeriod );
        reader.close();

        for ( String loan : map.keySet() ) {

            ArrayList<PerfData> al = map.get( loan );
            Collections.sort( al );
            char prev, curr;
            if ( al.size() > 1 )
                for ( int i = 0, j = al.size() - 1; i < j; i++ ) {
                    prev = al.get( i ).stts.charAt( 0 );
                    curr = al.get( i + 1 ).stts.charAt( 0 );

                    //System.out.println(prev + " " + curr);
                    try {
                        observationMatrix[( (char)prev ) - 65][( (char)curr ) - 65]++;
                    } catch( Exception | Error er ) {
                        //ex.printStackTrace();
                    }

                }
        }

        Map<Integer, ObservationMatrix> m_txMatrix = new HashMap<>( 500 );

        double[][] txMatrix = prepareTransitionMatrix( observationMatrix );
        m_txMatrix.put( 0, new ObservationMatrix( txMatrix ) );

        for ( int i = 1; i < 500; i++ ) {
            double[][] n_txMatrix = multiplyMatrices( txMatrix, m_txMatrix.get( i - 1 ).transitionMatrix );
            m_txMatrix.put( i, new ObservationMatrix( n_txMatrix ) );
        }

        printMatrix( observationMatrix );
        printMatrix( txMatrix );

        System.out.println( m_txMatrix );

        ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream( "d:/temp/myArr.ser" ) );
        oos.writeObject( m_txMatrix );
        oos.close();

        return m_txMatrix;
    }

    private LocalDate toDate( Date inputDt ) {
        return inputDt.toInstant().atZone( ZoneId.systemDefault() ).toLocalDate();
    }

    int getStartIndex( Row header, String startingQuarter ) {
        if ( header == null )
            return 2;
        if ( startingQuarter == null )
            return 2;

        startingQuarter += "_A";
        int colCount = 0;
        for ( Cell c : header ) {
            if ( startingQuarter.equalsIgnoreCase( c.getStringCellValue() ) )
                return colCount;
            else
                colCount++;
        }

        return 2;
    }

    int getEndIndex( Row header, String endingQuarter ) {
        if ( header == null )
            return 2;
        if ( endingQuarter == null )
            return 2;

        endingQuarter += "_A";

        int colCount = 0;
        for ( Cell c : header ) {
            if ( endingQuarter.equalsIgnoreCase( c.getStringCellValue() ) )
                return colCount;
            else
                colCount++;
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
            if ( getNumericCellValue( row.getCell( index ), -1d ) > 0 )
                return index;
        }
        return -1;
    }

    double getNumericCellValue( Cell cell, double defaultVal ) {

        try {
            return cell.getNumericCellValue();
        } catch( Exception ex ) {

        }

        return defaultVal;

/*        if ( cell != null && cell.getCellType() == CellType.NUMERIC )
    return cell.getNumericCellValue();
else
    return -1;*/
    }

    String getCellValue( Cell cell ) {
        /*switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return new BigDecimal( cell.getNumericCellValue() + "" ).toPlainString();
            default:
                return "";
        }*/

        try {
            return cell.getStringCellValue();
        } catch( Exception ex ) {}
        return "";
    }

    java.util.Date getDateValue( Cell cell ) {
        /*switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return new BigDecimal( cell.getNumericCellValue() + "" ).toPlainString();
            default:
                return "";
        }*/

        try {
            return cell.getDateCellValue();
        } catch( Exception ex ) {}
        return null;
    }

    static void printMatrix( int[][] matrix ) {
        for ( int i = 0; i < matrix.length; i++ ) {
            for ( int j = 0; j < matrix[i].length; j++ ) {
                System.out.print( matrix[i][j] + "," );
            }
            System.out.println( "\n" );
        }
    }

    static void printMatrix( double[][] matrix ) {
        for ( int i = 0; i < matrix.length; i++ ) {
            for ( int j = 0; j < matrix[i].length; j++ ) {
                System.out.print( matrix[i][j] + "," );
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

class PerfData implements Comparable<PerfData> {
    String loan, stts;
    LocalDate period;

    PerfData( String loan, String stts, LocalDate period ) {
        this.loan = loan;
        this.stts = stts;
        this.period = period;
    }

    public int compareTo( PerfData p ) {
        return period.compareTo( p.period );
    }

    public String toString() {
        return period.toString();
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
}

*
1118202     35463       24      17      10      42      0       0       

32309       57962       1397        168     97      109     0       0       

210     691     189     407     12      0       0       0       

58      108     25      45      299     3       0       0       

31      68      5       12      35      243     0       0       

49      86      4       7       11      2021        0       0       

0       0       0       0       0       0       0       0       

0       0       0       0       0       0       0       0       

0.9691824455388391      0.03073694830285034     2.0801589241418045E-5       1.4734459046004449E-5       8.667328850590852E-6        3.6402781172481574E-5       0.0     0.0     

0.3510245322787423      0.6297342517546337      0.015177853588579126        0.0018252536885334956       0.001053866713022316        0.0011842419764889942       0.0     0.0     

0.13916500994035785     0.4579191517561299      0.12524850894632206     0.26971504307488403     0.007952286282306162        0.0     0.0     0.0     

0.10780669144981413     0.20074349442379183     0.046468401486988845        0.08364312267657993     0.5557620817843866      0.0055762081784386614       0.0     0.0     

0.07868020304568528     0.17258883248730963     0.012690355329949238        0.030456852791878174        0.08883248730964467     0.616751269035533       0.0     0.0     

0.022497704315886134        0.03948576675849403     0.0018365472910927456       0.0032139577594123047       0.005050505050505051        0.9279155188246098      0.0     0.0     

0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     

0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     

{0=0.9691824455388391, 1=0.9501100198951833, 2=0.9381697673562461, 3=0.9306208503921363, 4=0.9257927329072818, 5=0.9226578437562686, 6=0.9205809399388517, 7=0.9191683289795218, 8=0.918175617280485, 9=0.917450853905569, 10=0.916899377989446, 11=0.9164620551223082, 12=0.9161017947593387, 13=0.9157951950488618, 14=0.9155273639479201, 15=0.9152887083815505, 16=0.9150729429524085, 17=0.9148758544412076, 18=0.9146945347351159, 19=0.9145269041158927, 20=0.9143714145619162, 21=0.9142268646822519, 22=0.9140922839041978, 23=0.9139668596490461, 24=0.9138498912157778, 25=0.9137407602799406, 26=0.9136389117493827, 27=0.9135438410948024, 28=0.9134550857458242, 29=0.9133722190561412, 30=0.9132948459071414, 31=0.9132225993703019, 32=0.9131551380662475, 33=0.9130921439934122, 34=0.9130333206830914, 35=0.9129783915898038, 36=0.9129270986583338, 37=0.9128792010290819, 38=0.9128344738560381, 39=0.9127927072196794, 40=0.9127537051221631, 41=0.9127172845554341, 42=0.9126832746349823, 43=0.9126515157933973, 44=0.9126218590288234, 45=0.9125941652041037, 46=0.9125683043928984, 47=0.9125441552694441, 48=0.9125216045389295, 49=0.9125005464057137, 50=0.9124808820768262, 51=0.9124625192983855, 52=0.9124453719227331, 53=0.9124293595042388, 54=0.9124144069218718, 55=0.9124004440267619, 56=0.9123874053130918, 57=0.9123752296107764, 58=0.9123638597984842, 59=0.912353242535655, 60=0.912343328012256, 61=0.9123340697151011, 62=0.9123254242096372, 63=0.9123173509361744, 64=0.9123098120196027, 65=0.9123027720917053, 66=0.91229619812523, 67=0.9122900592789452, 68=0.9122843267529503, 69=0.9122789736535636, 70=0.9122739748671526, 71=0.9122693069423153, 72=0.9122649479798605, 73=0.9122608775300683, 74=0.9122570764967532, 75=0.9122535270476752, 76=0.912250212530881, 77=0.9122471173965835, 78=0.9122442271242105, 79=0.912241528154283, 80=0.9122390078248024, 81=0.9122366543118471, 82=0.912234456574101, 83=0.9122324043010557, 84=0.9122304878646382, 85=0.9122286982740424, 86=0.9122270271335486, 87=0.9122254666031369, 88=0.9122240093617048, 89=0.9122226485727221, 90=0.9122213778521575, 91=0.9122201912385284, 92=0.9122190831649328, 93=0.9122180484329333, 94=0.9122170821881684, 95=0.9122161798975791, 96=0.9122153373281421, 97=0.9122145505270111, 98=0.912213815802972, 99=0.9122131297091268, 100=0.9122124890267217, 101=0.912211890750047, 102=0.9122113320723363, 103=0.9122108103725979, 104=0.91221032320332, 105=0.9122098682789872, 106=0.912209443465359, 107=0.9122090467694567, 108=0.9122086763302133, 109=0.9122083304097433, 110=0.9122080073851888, 111=0.9122077057411062, 112=0.9122074240623568, 113=0.9122071610274675, 114=0.9122069154024313, 115=0.9122066860349182, 116=0.9122064718488693, 117=0.9122062718394479, 118=0.9122060850683261, 119=0.9122059106592822, 120=0.9122057477940904, 121=0.9122055957086818, 122=0.91220545368956, 123=0.9122053210704537, 124=0.9122051972291912, 125=0.9122050815847814, 126=0.9122049735946876, 127=0.912204872752283, 128=0.9122047785844739, 129=0.9122046906494794, 130=0.9122046085347598, 131=0.9122045318550805, 132=0.912204460250705, 133=0.9122043933857071, 134=0.9122043309463951, 135=0.9122042726398398, 136=0.9122042181925011, 137=0.9122041673489435, 138=0.9122041198706387, 139=0.9122040755348457, 140=0.9122040341335672, 141=0.9122039954725721, 142=0.9122039593704858, 143=0.9122039256579381, 144=0.9122038941767693, 145=0.9122038647792879, 146=0.9122038373275784, 147=0.9122038116928529, 148=0.9122037877548486, 149=0.9122037654012618, 150=0.9122037445272226, 151=0.9122037250348022, 152=0.9122037068325537, 153=0.9122036898350824, 154=0.9122036739626462, 155=0.9122036591407808, 156=0.9122036452999508, 157=0.9122036323752228, 158=0.9122036203059618, 159=0.9122036090355455, 160=0.9122035985110999, 161=0.9122035886832501, 162=0.9122035795058899, 163=0.9122035709359644, 164=0.9122035629332683, 165=0.9122035554602577, 166=0.9122035484818735, 167=0.9122035419653772, 168=0.9122035358801974, 169=0.9122035301977858, 170=0.9122035248914836, 171=0.912203519936397, 172=0.9122035153092797, 173=0.9122035109884237, 174=0.9122035069535581, 175=0.9122035031857537, 176=0.9122034996673343, 177=0.9122034963817933, 178=0.9122034933137168, 179=0.9122034904487114, 180=0.912203487773336, 181=0.9122034852750395, 182=0.9122034829421011, 183=0.912203480763576, 184=0.9122034787292441, 185=0.9122034768295614, 186=0.9122034750556154, 187=0.9122034733990841, 188=0.9122034718521961, 189=0.912203470407694, 190=0.9122034690588012, 191=0.9122034677991894, 192=0.9122034666229493, 193=0.9122034655245626, 194=0.9122034644988762, 195=0.9122034635410784, 196=0.9122034626466757, 197=0.9122034618114719, 198=0.9122034610315491, 199=0.9122034603032481, 200=0.9122034596231521, 201=0.9122034589880705, 202=0.9122034583950238, 203=0.91220345784123, 204=0.9122034573240911, 205=0.9122034568411808, 206=0.9122034563902334, 207=0.9122034559691334, 208=0.9122034555759054, 209=0.9122034552087045, 210=0.912203454865808, 211=0.9122034545456074, 212=0.9122034542466002, 213=0.912203453967384, 214=0.9122034537066487, 215=0.9122034534631709, 216=0.9122034532358084, 217=0.9122034530234948, 218=0.912203452825234, 219=0.9122034526400955, 220=0.9122034524672111, 221=0.9122034523057698, 222=0.9122034521550141, 223=0.9122034520142365, 224=0.9122034518827766, 225=0.912203451760018, 226=0.9122034516453846, 227=0.9122034515383386, 228=0.9122034514383777, 229=0.9122034513450332, 230=0.912203451257867, 231=0.9122034511764701, 232=0.9122034511004607, 233=0.9122034510294822, 234=0.9122034509632018, 235=0.9122034509013082, 236=0.9122034508435113, 237=0.9122034507895399, 238=0.9122034507391408, 239=0.9122034506920774, 240=0.9122034506481294, 241=0.9122034506070898, 242=0.9122034505687668, 243=0.9122034505329804, 244=0.9122034504995623, 245=0.9122034504683563, 246=0.9122034504392158, 247=0.9122034504120041, 248=0.9122034503865935, 249=0.9122034503628648, 250=0.9122034503407066, 251=0.912203450320015, 252=0.912203450300693, 253=0.9122034502826498, 254=0.912203450265801, 255=0.9122034502500672, 256=0.912203450235375, 257=0.9122034502216549, 258=0.9122034502088432, 259=0.9122034501968793, 260=0.9122034501857074, 261=0.912203450175275, 262=0.9122034501655332, 263=0.9122034501564359, 264=0.9122034501479409, 265=0.912203450140008, 266=0.9122034501326003, 267=0.9122034501256828, 268=0.9122034501192234, 269=0.9122034501131915, 270=0.9122034501075587, 271=0.9122034501022986, 272=0.9122034500973869, 273=0.9122034500928001, 274=0.912203450088517, 275=0.9122034500845172, 276=0.9122034500807823, 277=0.9122034500772946, 278=0.9122034500740378, 279=0.9122034500709965, 280=0.9122034500681564, 281=0.9122034500655044, 282=0.9122034500630278, 283=0.9122034500607155, 284=0.912203450058556, 285=0.9122034500565395, 286=0.9122034500546564, 287=0.912203450052898, 288=0.9122034500512559, 289=0.9122034500497226, 290=0.9122034500482906, 291=0.9122034500469534, 292=0.9122034500457048, 293=0.9122034500445387, 294=0.9122034500434499, 295=0.9122034500424332, 296=0.9122034500414837, 297=0.9122034500405972, 298=0.9122034500397692, 299=0.9122034500389962, 300=0.9122034500382743, 301=0.9122034500376002, 302=0.9122034500369705, 303=0.9122034500363826, 304=0.9122034500358337, 305=0.912203450035321, 306=0.9122034500348422, 307=0.912203450034395, 308=0.9122034500339775, 309=0.9122034500335876, 310=0.9122034500332237, 311=0.9122034500328837, 312=0.9122034500325664, 313=0.9122034500322701, 314=0.9122034500319933, 315=0.9122034500317348, 316=0.9122034500314934, 317=0.9122034500312681, 318=0.9122034500310576, 319=0.9122034500308611, 320=0.9122034500306775, 321=0.912203450030506, 322=0.9122034500303459, 323=0.9122034500301963, 324=0.9122034500300568, 325=0.9122034500299264, 326=0.9122034500298047, 327=0.912203450029691, 328=0.912203450029585, 329=0.912203450029486, 330=0.9122034500293934, 331=0.912203450029307, 332=0.9122034500292263, 333=0.9122034500291509, 334=0.9122034500290805, 335=0.9122034500290149, 336=0.9122034500289535, 337=0.9122034500288964, 338=0.9122034500288428, 339=0.9122034500287928, 340=0.912203450028746, 341=0.9122034500287024, 342=0.9122034500286617, 343=0.9122034500286238, 344=0.9122034500285883, 345=0.9122034500285551, 346=0.9122034500285241, 347=0.9122034500284952, 348=0.9122034500284683, 349=0.912203450028443, 350=0.9122034500284195, 351=0.9122034500283975, 352=0.912203450028377, 353=0.912203450028358, 354=0.9122034500283401, 355=0.9122034500283234, 356=0.9122034500283078, 357=0.9122034500282932, 358=0.9122034500282796, 359=0.9122034500282669, 360=0.912203450028255, 361=0.9122034500282439, 362=0.9122034500282336, 363=0.9122034500282238, 364=0.9122034500282147, 365=0.9122034500282062, 366=0.9122034500281982, 367=0.9122034500281908, 368=0.9122034500281839, 369=0.9122034500281775, 370=0.9122034500281715, 371=0.9122034500281659, 372=0.9122034500281607, 373=0.912203450028156, 374=0.9122034500281515, 375=0.9122034500281473, 376=0.9122034500281434, 377=0.9122034500281397, 378=0.9122034500281363, 379=0.9122034500281332, 380=0.9122034500281303, 381=0.9122034500281275, 382=0.9122034500281249, 383=0.9122034500281224, 384=0.9122034500281202, 385=0.912203450028118, 386=0.912203450028116, 387=0.912203450028114, 388=0.9122034500281122, 389=0.9122034500281104, 390=0.9122034500281089, 391=0.9122034500281074, 392=0.9122034500281061, 393=0.9122034500281049, 394=0.9122034500281037, 395=0.9122034500281025, 396=0.9122034500281015, 397=0.9122034500281005, 398=0.9122034500280997, 399=0.9122034500280988, 400=0.912203450028098, 401=0.9122034500280972, 402=0.9122034500280966, 403=0.9122034500280959, 404=0.9122034500280952, 405=0.9122034500280946, 406=0.912203450028094, 407=0.9122034500280934, 408=0.912203450028093, 409=0.9122034500280926, 410=0.9122034500280921, 411=0.9122034500280917, 412=0.9122034500280912, 413=0.9122034500280908, 414=0.9122034500280903, 415=0.9122034500280899, 416=0.9122034500280896, 417=0.9122034500280892, 418=0.9122034500280889, 419=0.9122034500280886, 420=0.9122034500280883, 421=0.9122034500280881, 422=0.9122034500280879, 423=0.9122034500280877, 424=0.9122034500280874, 425=0.9122034500280872, 426=0.912203450028087, 427=0.9122034500280868, 428=0.9122034500280866, 429=0.9122034500280864, 430=0.9122034500280863, 431=0.9122034500280862, 432=0.9122034500280861, 433=0.912203450028086, 434=0.9122034500280859, 435=0.9122034500280858, 436=0.9122034500280857, 437=0.9122034500280856, 438=0.9122034500280854, 439=0.9122034500280853, 440=0.9122034500280852, 441=0.9122034500280851, 442=0.912203450028085, 443=0.9122034500280849, 444=0.9122034500280848, 445=0.9122034500280847, 446=0.9122034500280846, 447=0.9122034500280845, 448=0.9122034500280843, 449=0.9122034500280842, 450=0.9122034500280841, 451=0.912203450028084, 452=0.9122034500280839, 453=0.9122034500280838, 454=0.9122034500280837, 455=0.9122034500280836, 456=0.9122034500280835, 457=0.9122034500280833, 458=0.9122034500280832, 459=0.9122034500280831, 460=0.912203450028083, 461=0.9122034500280828, 462=0.9122034500280827, 463=0.9122034500280826, 464=0.9122034500280825, 465=0.9122034500280823, 466=0.9122034500280822, 467=0.9122034500280821, 468=0.912203450028082, 469=0.9122034500280819, 470=0.9122034500280818, 471=0.9122034500280817, 472=0.9122034500280816, 473=0.9122034500280815, 474=0.9122034500280813, 475=0.9122034500280812, 476=0.9122034500280811, 477=0.912203450028081, 478=0.9122034500280809, 479=0.9122034500280808, 480=0.9122034500280807, 481=0.9122034500280806, 482=0.9122034500280805, 483=0.9122034500280803, 484=0.9122034500280802, 485=0.9122034500280801, 486=0.91220345002808, 487=0.9122034500280799, 488=0.9122034500280798, 489=0.9122034500280797, 490=0.9122034500280796, 491=0.9122034500280795, 492=0.9122034500280793, 493=0.9122034500280793, 494=0.9122034500280792, 495=0.9122034500280791, 496=0.912203450028079, 497=0.9122034500280789, 498=0.9122034500280788, 499=0.9122034500280787}
74011ms

done

*/
