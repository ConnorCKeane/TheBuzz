package edu.lehigh.cse216.cck226.backend;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Date;

public class Database {
    /**
     * The connection to the database.  When there is no connection, it should
     * be null.  Otherwise, there is a valid open connection
     */
    private Connection mConnection;

    /**
     * A prepared statement for getting all data in the database
     */
    private PreparedStatement mSelectAll;

    /**
     * A prepared statement for getting one row from the database
     */
    private PreparedStatement mSelectOne;

    /**
     * A prepared statement for deleting a row from the database
     */
    private PreparedStatement mDeleteOne;

    /**
     * A prepared statement for inserting into the database
     */
    private PreparedStatement mInsertOne;

    /**
     * A prepared statement for updating a single row in the database
     */
    private PreparedStatement mUpdateOne;

    /**
     * A prepared statement for creating the table in our database
     */
    private PreparedStatement mCreateTable;

    /**
     * A prepared statement for dropping the table in our database
     */
    private PreparedStatement mDropTable;

    public class DataRow {
        /**
         * The unique identifier associated with this element.  It's final, because
         * we never want to change it.
         */
        public final int mId;

        /**
         * The title for this row of data
         */
        public String mTitle;

        /**
         * The content for this row of data
         */
        public String mContent;

        /**
         * The creation date for this row of data.  Once it is set, it cannot be 
         * changed
         */
        public final Date mCreated;

        /**
         * Create a new DataRow with the provided id and title/content, and a 
         * creation date based on the system clock at the time the constructor was
         * called
         * 
         * @param id The id to associate with this row.  Assumed to be unique 
         *           throughout the whole program.
         * 
         * @param title The title string for this row of data
         * 
         * @param content The content string for this row of data
         */
        DataRow(int id, String title, String content) {
            mId = id;
            mTitle = title;
            mContent = content;
            mCreated = new Date();
        }

        /**
         * Copy constructor to create one datarow from another
         */
        DataRow(DataRow data) {
            mId = data.mId;
            // NB: Strings and Dates are immutable, so copy-by-reference is safe
            mTitle = data.mTitle;
            mContent = data.mContent;
            mCreated = data.mCreated;
        }
    }

    /**
     * The Database constructor is private: we only create Database objects 
     * through the getDatabase() method.
     */
    private Database() {
    }


    private void createPreparedStatements() {
        try {
            // NB: we can easily get ourselves in trouble here by typing the
            //     SQL incorrectly.  We really should have things like "tblData"
            //     as constants, and then build the strings for the statements
            //     from those constants.
            // Note: no "IF NOT EXISTS" or "IF EXISTS" checks on table
            // creation/deletion, so multiple executions will cause an exception
            mCreateTable = mConnection.prepareStatement(
                    "CREATE TABLE tblData (id SERIAL PRIMARY KEY, subject VARCHAR(50) "
                    + "NOT NULL, message VARCHAR(500) NOT NULL)");
            mDropTable = mConnection.prepareStatement("DROP TABLE tblData");
            // Standard CRUD operations
            mDeleteOne = mConnection.prepareStatement("DELETE FROM tblData WHERE id = ?");
            mInsertOne = mConnection.prepareStatement("INSERT INTO tblData VALUES (default, ?, ?)");
            mSelectAll = mConnection.prepareStatement("SELECT id, subject FROM tblData");
            mSelectOne = mConnection.prepareStatement("SELECT * from tblData WHERE id=?");
            mUpdateOne = mConnection.prepareStatement("UPDATE tblData SET message = ? WHERE id = ?");
        } catch (SQLException e) {
            System.err.println("Error creating prepared statement");
            e.printStackTrace();
            disconnect();
        }
    }


    /**
    * Get a fully-configured connection to the database
    * 
    * @param host The IP address or hostname of the database server
    * @param port The port on the database server to which connection requests
    *             should be sent
    * @param path The path to use, can be null
    * @param user The user ID to use when connecting
    * @param pass The password to use when connecting
    * 
    * @return A Database object, or null if we cannot connect properly
    */
    static Database getDatabase(String host, String port, String path, String user, String pass) {
        if( path==null || "".equals(path) ){
            path="/";
        }

        // Create an un-configured Database object
        Database db = new Database();

        // Give the Database object a connection, fail if we cannot get one
        try {
            String dbUrl = "jdbc:postgresql://" + host + ':' + port + path;
            Connection conn = DriverManager.getConnection(dbUrl, user, pass);
            if (conn == null) {
                System.err.println("Error: DriverManager.getConnection() returned a null object");
                return null;
            }
            db.mConnection = conn;
        } catch (SQLException e) {
            System.err.println("Error: DriverManager.getConnection() threw a SQLException");
            e.printStackTrace();
            return null;
        }

        db.createPreparedStatements();
        return db;
    } 

    /**
    * Get a fully-configured connection to the database
    * 
    * @param db_url The url to the database
    * @param port_default port to use if absent in db_url
    * 
    * @return A Database object, or null if we cannot connect properly
    */
    static Database getDatabase(String db_url, String port_default) {
        try {
            URI dbUri = new URI(db_url);
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String host = dbUri.getHost();
            String path = dbUri.getPath();
            String port = dbUri.getPort() == -1 ? port_default : Integer.toString(dbUri.getPort());

            return getDatabase(host, port, path, username, password);
        } catch (URISyntaxException s) {
            System.out.println("URI Syntax Error");
            return null;
        }
    }

    /**
     * Close the current connection to the database, if one exists.
     * 
     * NB: The connection will always be null after this call, even if an 
     *     error occurred during the closing operation.
     * 
     * @return True if the connection was cleanly closed, false otherwise
     */
    boolean disconnect() {
        if (mConnection == null) {
            System.err.println("Unable to close connection: Connection was null");
            return false;
        }
        try {
            mConnection.close();
        } catch (SQLException e) {
            System.err.println("Error: Connection.close() threw a SQLException");
            e.printStackTrace();
            mConnection = null;
            return false;
        }
        mConnection = null;
        return true;
    }

    /**
     * Insert a row into the database
     * 
     * @param subject The subject for this new row
     * @param message The message body for this new row
     * 
     * @return The number of rows that were inserted
     */
    int insertRow(String subject, String message) {
        int count = 0;
        try {
            mInsertOne.setString(1, subject);
            mInsertOne.setString(2, message);
            count += mInsertOne.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Query the database for a list of all subjects and their IDs
     * 
     * @return All rows, as an ArrayList
     */
    ArrayList<DataRow> selectAll() {
        ArrayList<DataRow> res = new ArrayList<DataRow>();
        try {
            ResultSet rs = mSelectAll.executeQuery();
            while (rs.next()) {
                res.add(new DataRow(rs.getInt("id"), rs.getString("subject"), null));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all data for a specific row, by ID
     * 
     * @param id The id of the row being requested
     * 
     * @return The data for the requested row, or null if the ID was invalid
     */
    DataRow selectOne(int id) {
        DataRow res = null;
        try {
            mSelectOne.setInt(1, id);
            ResultSet rs = mSelectOne.executeQuery();
            if (rs.next()) {
                res = new DataRow(rs.getInt("id"), rs.getString("subject"), rs.getString("message"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Delete a row by ID
     * 
     * @param id The id of the row to delete
     * 
     * @return The number of rows that were deleted.  -1 indicates an error.
     */
    int deleteRow(int id) {
        int res = -1;
        try {
            mDeleteOne.setInt(1, id);
            res = mDeleteOne.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Update the message for a row in the database
     * 
     * @param id The id of the row to update
     * @param message The new message contents
     * 
     * @return The number of rows that were updated.  -1 indicates an error.
     */
    int updateOne(int id, String message) {
        int res = -1;
        try {
            mUpdateOne.setString(1, message);
            mUpdateOne.setInt(2, id);
            res = mUpdateOne.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Create tblData.  If it already exists, this will print an error
     */
    void createTable() {
        try {
            mCreateTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove tblData from the database.  If it does not exist, this will print
     * an error.
     */
    void dropTable() {
        try {
            mDropTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
}
