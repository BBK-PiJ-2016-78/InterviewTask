import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class Launcher {

  public static void main(String[] args) {
    Connection conn = DBConnector.connect();
    DBCreator creator = new DBCreator(conn);
    Launcher launcher = new Launcher();
    ExportHTML html = new ExportHTML();

    String data;
    try {
      creator.createTable();
      data = launcher.menu(creator);
      creator.selectRows();
      html.writeData(data);
      html.openHTML();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Creates a console menu with the 4 data loading options
   * @param creator the DBCreator instance.
   * @return the data to be written in the HTML file.
   * @throws Exception if loadCSV fails.
   */
  private String menu(DBCreator creator) throws Exception {

    String data;
    Scanner sc = new Scanner(System.in);

    System.out.println(" _________________________________________________________________");
    System.out.println("| To load the CSV file into database choose option:               |");
    System.out.println("| 1. Load it row by row with committing separate INSERT statements|");
    System.out.println("| 2. Load it row by row but commit as a unit                      |");
    System.out.println("| 3. Load it in a single batch unit                               |");
    System.out.println("| 4. Load it in multiple batches of 1000 size                     |");
    System.out.println("|_________________________________________________________________|");
    System.out.print("Enter choice: ");
    int option = sc.nextInt();

    switch (option) {
      case 1: data = creator.loadCSV(true, false, false, true);
              break;
      case 2: data = creator.loadCSV(true, false, false, false);
              break;
      case 3: data = creator.loadCSV(false, true, false, false);
              break;
      case 4: data = creator.loadCSV(false, false, true, false);
              break;
      default: System.out.println("Invalid input!");
               data = menu(creator);
               break;
    }

    return data;
  }
}
