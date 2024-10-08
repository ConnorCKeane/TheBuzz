package edu.lehigh.cse216.cck226.backend;

import java.util.ArrayList;

/**
 * DataStore provides access to a set of objects, and makes sure that each has
 * a unique identifier that remains unique even after the object is deleted.
 * 
 * We follow the convention that member fields of a class have names that start
 * with a lowercase 'm' character, and are in camelCase.
 * 
 * NB: The methods of DataStore are synchronized, since they will be used from a 
 * web framework and there may be multiple concurrent accesses to the DataStore.
 */
public class DataStore {
    /**
     * The rows of data in our DataStore
     */
    private ArrayList<DataRow> mRows;

    /**
     * A counter for keeping track of the next ID to assign to a new row
     */
    private int mCounter;

    /**
     * Construct the DataStore by resetting its counter and creating the
     * ArrayList for the rows of data.
     */
    DataStore() {
        mCounter = 0;
        mRows = new ArrayList<>();
    }

    /**
     * Add a new row to the DataStore
     * 
     * Note: we return -1 on an error.  There are many good ways to handle an 
     * error, to include throwing an exception.  In robust code, returning -1 
     * may not be the most appropriate technique, but it is sufficient for this 
     * tutorial.
     * 
     * @param title The title for this newly added row
     * @param content The content for this row
     * @return the ID of the new row, or -1 if no row was created
     */
    public synchronized int createEntry(String title, String content) {
        if (title == null || content == null)
            return -1;
        // NB: we can safely assume that id is greater than the largest index in 
        //     mRows, and thus we can use the index-based add() method
        int id = mCounter++;
        DataRow data = new DataRow(id, title, content);
        mRows.add(id, data);
        return id;
    }

    /**
     * Get one complete row from the DataStore using its ID to select it
     * 
     * @param id The id of the row to select
     * @return A copy of the data in the row, if it exists, or null otherwise
     */
    public synchronized DataRow readOne(int id) {
        if (id >= mRows.size())
            return null;
        DataRow data = mRows.get(id);
        if (data == null)
            return null;
        return new DataRow(data);
    }

    /**
     * Get all of the ids and titles that are present in the DataStore
     * @return An ArrayList with all of the data
     */
    public synchronized ArrayList<DataRowLite> readAll() {
        ArrayList<DataRowLite> data = new ArrayList<>();
        // NB: we copy the data, so that our ArrayList only has ids and titles
        for (DataRow row : mRows) {
            if (row != null)
                data.add(new DataRowLite(row));
        }
        return data;
    }

    /**
     * Update the title and content of a row in the DataStore
     *
     * @param id The Id of the row to update
     * @param title The new title for the row
     * @param content The new content for the row
     * @return a copy of the data in the row, if it exists, or null otherwise
     */
    public synchronized DataRow updateOne(int id, String title, String content) {
        // Do not update if we don't have valid data
        if (title == null || content == null)
            return null;
        // Only update if the current entry is valid (not null)
        if (id >= mRows.size())
            return null;
        DataRow data = mRows.get(id);
        if (data == null)
            return null;
        // Update and then return a copy of the data, as a DataRow
        data.mTitle = title;
        data.mContent = content;
        return new DataRow(data);
    }

    /**
     * Delete a row from the DataStore
     * 
     * @param id The Id of the row to delete
     * @return true if the row was deleted, false otherwise
     */
    public synchronized boolean deleteOne(int id) {
        // Deletion fails for an invalid Id or an Id that has already been 
        // deleted
        if (id >= mRows.size())
            return false;
        if (mRows.get(id) == null)
            return false;
        // Delete by setting to null, so that any Ids used by other clients
        // still refer to the same positions in the ArrayList.
        mRows.set(id, null);
        return true;
    }
}
