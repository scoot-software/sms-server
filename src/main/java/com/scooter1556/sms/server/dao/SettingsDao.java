/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.server.dao;

import com.scooter1556.sms.server.database.SettingsDatabase;
import com.scooter1556.sms.server.database.SettingsDatabase.MediaFolderMapper;
import com.scooter1556.sms.server.domain.MediaFolder;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.stereotype.Component;

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
            settingsDatabase.getJdbcTemplate().update("INSERT INTO MediaFolder (ID,Name,Path) "
                    + "VALUES (?,?,?)", new Object[]{mediaFolder.getID(), mediaFolder.getName(), mediaFolder.getPath()});
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }

    public boolean removeMediaFolder(UUID id) {
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

    public List<MediaFolder> getMediaFolders(Byte type) {
        try {
            List<MediaFolder> mediaFolders;
            
            if(type == null) {
                mediaFolders = settingsDatabase.getJdbcTemplate().query("SELECT * FROM MediaFolder", new MediaFolderMapper());
            } else {
                mediaFolders = settingsDatabase.getJdbcTemplate().query("SELECT * FROM MediaFolder WHERE Type=?", new MediaFolderMapper(), new Object[]{type});
            }
            
            return mediaFolders;
        } catch (DataAccessException e) {
            return null;
        }
    }

    public MediaFolder getMediaFolderByID(UUID id) {
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
        try {
            if (!path.isEmpty()) {

                List<MediaFolder> mediaFolders = settingsDatabase.getJdbcTemplate().query("SELECT * FROM MediaFolder WHERE Path=?", new MediaFolderMapper(), new Object[]{path});

                if (mediaFolders != null && !mediaFolders.isEmpty()) {
                    return mediaFolders.get(0);
                }
            }
        } catch (DataAccessException e) {
            return null;
        }
        
        return null;
    }
}
