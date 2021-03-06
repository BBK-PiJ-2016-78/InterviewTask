import com.jamonapi.MonitorFactory;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;

public class DBCreator {

  private final Connection conn;

  private static final String SQL_INSERT = "INSERT INTO FL_INSURANCE (${keys}) VALUES(${values})";
  private static final String KEYS_REGEX = "\\$\\{keys\\}";
  private static final String VALUES_REGEX = "\\$\\{values\\}";

  public DBCreator(Connection conn) {
    this.conn = conn;
  }

  /**
   * Creates a table in the database with the given schema. If table exists truncate it.
   *
   * @throws SQLException if creating the table fails.
   */
  public void createTable() throws SQLException {

    String createString = "CREATE TABLE FL_INSURANCE  "
        + "(policyID INT NOT NULL"
        + "   CONSTRAINT policyID PRIMARY KEY, "
        + "statecode VARCHAR(32) NOT NULL, "
        + "county VARCHAR(32) NOT NULL, "
        + "eq_site_limit FLOAT NOT NULL, "
        + "hu_site_limit FLOAT NOT NULL, "
        + "fl_site_limit FLOAT NOT NULL, "
        + "fr_site_limit FLOAT NOT NULL, "
        + "tiv_2011 FLOAT NOT NULL, "
        + "tiv_2012 FLOAT NOT NULL, "
        + "eq_site_deductible FLOAT NOT NULL, "
        + "hu_site_deductible FLOAT NOT NULL,"
        + "fl_site_deductible FLOAT NOT NULL,"
        + "fr_site_deductible FLOAT NOT NULL,"
        + "point_latitude FLOAT NOT NULL,"
        + "point_longitude FLOAT NOT NULL,"
        + "line VARCHAR(32) NOT NULL,"
        + "construction VARCHAR(32) NOT NULL,"
        + "point_granularity INT NOT NULL)";


    PreparedStatement preparedStm = null;
    try {
      preparedStm = conn.prepareStatement(createString);
      DatabaseMetaData meta = conn.getMetaData();
      ResultSet result = meta.getTables(null, null, "FL_INSURANCE", null);
      if (result.next()) {
        if (result.getString(3).equals("FL_INSURANCE")) {
          System.out.println("Table FL_INSURANCE exists!");
          System.out.println("Truncating table . . .");
          PreparedStatement del = conn.prepareStatement("TRUNCATE TABLE FL_INSURANCE");
          del.execute();
          del.close();
        }
      } else {
        System.out.println(" . . . . creating table FL_INSURANCE");
        preparedStm.execute();
      }
      result.close();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      if (preparedStm != null) {
        preparedStm.close();
      }
    }
  }

  /**
   * Loads the CSV file into the database with prepared INSERT statements, with 4 different
   * performance options: 1/2) row by row as unit or without; 3) batch add; 4) batch in batch.
   *
   * @param eachRow      ff set True then row by row INSERT is used.
   * @param singleBatch  if set to true single batch commit is used.
   * @param batchInBatch if set to true batch within batch is used.
   * @param autoCommit   set on/off autocommit (true only with eachRow).
   * @return Returns the Object data from the Monitor report.
   * @throws Exception if reading the file fails.
   */
  public String loadCSV(boolean eachRow, boolean singleBatch,
                        boolean batchInBatch, boolean autoCommit) throws Exception {
    CSVReader csvreader;
    String filePath = "FL_insurance_sample.csv";
    //open the file from within the jar as inout stream.
    InputStream is = getClass().getResourceAsStream(filePath);

    //open and parse the CSV file using the inout stream from jar.
    csvreader = new CSVReader(new InputStreamReader(is));
    //check if file exists by reading the header line.
    String[] headerRow = null;
    if (csvreader != null) {
      headerRow = csvreader.readNext();
      if (headerRow == null) {
        throw new FileNotFoundException(
            "No columns defined in given CSV file." + "Please check the CSV file format.");
      }
    }
    //Create as many ?'s as the columns in the table.
    String questionmarks = StringUtils.repeat("?,", headerRow.length);
    //Create sequence with the question marks minus 1.
    questionmarks = (String) questionmarks.subSequence(0, questionmarks.length() - 1);
    String query = SQL_INSERT;
    //Replace the keys regular expression with column names from the header separated by comma.
    query = query.replaceFirst(KEYS_REGEX, StringUtils.join(headerRow, ","));
    //Replace the values regular expression with the question marks.
    query = query.replaceFirst(VALUES_REGEX, questionmarks);

    String[] row;
    conn.setAutoCommit(autoCommit);
    PreparedStatement ps = conn.prepareStatement(query);

    int batchSize = 1000; //set the batch size for batchInBatch to 1000 rows per batch
    int count = 0;

    while ((row = csvreader.readNext()) != null) {
      int index = 1;
      for (String value : row) {
        ps.setString(index++, value); //replace the ?'s with the rows values from the file
      }
      if (eachRow) {
        ps.executeUpdate(); //execute each insert statement
      } else if (singleBatch && !autoCommit) {
        ps.addBatch(); //add statements to execute in batch
      } else if (batchInBatch && !autoCommit) {
        ps.addBatch();
        if (++count % batchSize == 0) {
          ps.executeBatch(); //execute the current batch size
        }
      }
    }
    if (eachRow && !autoCommit) {
      conn.commit(); //commit as a unit
    } else if (!autoCommit) {
      ps.executeBatch(); //insert the remaining rows (all of the for single batch)
      conn.commit();
    }
    //get the Monitor report data as a 2D Object array
    String data = MonitorFactory.getReport();
    ps.close();
    csvreader.close();

    return data;
  }

  /**
   * Used for testing purposes. Prints the first 10 rows from the database.
   *
   * @throws SQLException if selecting the rows fails.
   */
  public void selectRows() throws SQLException {

    String query = "SELECT * FROM FL_INSURANCE";
    conn.setAutoCommit(true);
    try {
      PreparedStatement ps = conn.prepareStatement(query);
      ResultSet rs = ps.executeQuery();
      int columnCount = rs.getMetaData().getColumnCount();
      String border = StringUtils.repeat("_", 10 * columnCount);
      System.out.println("\nPrinting first 10 rows of the table . . . .\n");
      System.out.println(border);
      for (int i = 0; i < 10; i++) {
        rs.next();
        String rows = "";
        for (int j = 1; j < columnCount + 1; j++) {
          rows += rs.getString(j) + " | ";
        }
        System.out.println(rows);
      }
      System.out.println(border + "\n");
      ps.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
