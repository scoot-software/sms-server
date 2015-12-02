/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.dao;

import com.scooter1556.sms.server.database.SettingsDatabase;
import com.scooter1556.sms.server.domain.MediaFolder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 *
 * @author scott2ware
 */
@Component
public class SettingsDao {

    @Autowired
    private SettingsDatabase settingsDatabase;

    private static final String CLASS_NAME = "SettingsDao";

    //
    // Media Folders
    //
    public boolean createMediaFolder(MediaFolder mediaFolder) {
        try {
            settingsDatabase.getJdbcTemplate().update("INSERT INTO MediaFolder (Name,Type,Path) "
                    + "VALUES (?,?,?)", new Object[]{mediaFolder.getName(), mediaFolder.getType(), mediaFolder.getPath()});
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }

    public boolean removeMediaFolder(Long id) {
        try {
            settingsDatabase.getJdbcTemplate().update("DELETE FROM MediaFolder WHERE ID=?", id);
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }

    public boolean updateMediaFolder(MediaFolder mediaFolder) {
        try {
            settingsDatabase.getJdbcTemplate().update("UPDATE MediaFolder SET Name=?, Type=?, Path=?, Folders=?, Files=?, LastScanned=?, Enabled=? WHERE ID=?",
                    new Object[]{mediaFolder.getName(), mediaFolder.getType(), mediaFolder.getPath(), mediaFolder.getFolders(), mediaFolder.getFiles(), mediaFolder.getLastScanned(), mediaFolder.getEnabled(),
                        mediaFolder.getID()});
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }

    public List<MediaFolder> getMediaFolders() {
        try {
            List<MediaFolder> mediaFolders = settingsDatabase.getJdbcTemplate().query("SELECT * FROM MediaFolder", new MediaFolderMapper());
            return mediaFolders;
        } catch (DataAccessException e) {
            return null;
        }
    }

    public MediaFolder getMediaFolderByID(Long id) {
        MediaFolder mediaFolder = null;

        try {
            if (id != null) {

                List<MediaFolder> mediaFolders = settingsDatabase.getJdbcTemplate().query("SELECT * FROM MediaFolder WHERE ID=?", new MediaFolderMapper(), new Object[]{id});

                if (mediaFolders != null) {
                    if (mediaFolders.size() > 0) {
                        mediaFolder = mediaFolders.get(0);
                    }
                }
            }
        } catch (DataAccessException e) {
            return null;
        }

        return mediaFolder;
    }

    public MediaFolder getMediaFolderByPath(String path) {
        MediaFolder mediaFolder = null;

        try {
            if (!path.equals("")) {

                List<MediaFolder> mediaFolders = settingsDatabase.getJdbcTemplate().query("SELECT * FROM MediaFolder WHERE Path=?", new MediaFolderMapper(), new Object[]{path});

                if (mediaFolders != null) {
                    if (mediaFolders.size() > 0) {
                        mediaFolder = mediaFolders.get(0);
                    }
                }
            }
        } catch (DataAccessException e) {
            return null;
        }

        return mediaFolder;
    }
    
    public boolean forceRescan(Long id)
    {
        try {
            if(id == null) {
                settingsDatabase.getJdbcTemplate().update("UPDATE MediaFolder SET LastScanned=NULL");
            } else {
                settingsDatabase.getJdbcTemplate().update("UPDATE MediaFolder SET LastScanned=NULL WHERE ID=?", new Object[] {id});
            }
        }
        catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }

    private static final class MediaFolderMapper implements RowMapper {

        @Override
        public MediaFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFolder mediaFolder = new MediaFolder();
            mediaFolder.setID(rs.getLong("ID"));
            mediaFolder.setName(rs.getString("Name"));
            mediaFolder.setType(rs.getByte("Type"));
            mediaFolder.setPath(rs.getString("Path"));
            mediaFolder.setFolders(rs.getLong("Folders"));
            mediaFolder.setFiles(rs.getLong("Files"));
            mediaFolder.setLastScanned(rs.getTimestamp("LastScanned"));
            mediaFolder.setEnabled(rs.getBoolean("Enabled"));
            return mediaFolder;
        }
    }
}
