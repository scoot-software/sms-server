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
package com.scooter1556.sms.server.domain;

import com.scooter1556.sms.server.SMS;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.UUID;

@ApiModel(description = "User rule")
public class UserRuleRequest implements Serializable {
    
    @ApiModelProperty(value = "Username associated with rule", required = true, example = "smsuser")
    private String username;
    
    @ApiModelProperty(value = "ID of media element or folder", required = true, example = "53a210b9-3f65-438b-a5c6-d177ed0fb651")
    private UUID id;

    @ApiModelProperty(value = "Rule", required = true, allowableValues = "0, 1", example = "0")
    private Byte rule;
    
    @ApiModelProperty(value = "Whether this request is associated with a media folder", required = false, example = "false")
    private Boolean folder;

    public UserRuleRequest() {};
    
    public UserRuleRequest(String username, UUID id, Byte rule, Boolean folder)
    {
        this.username = username;
        this.id = id;
        this.rule = rule;
        this.folder = folder;
    }
    
    @Override
    public String toString() {
        return String.format(
                "User[Username=%s, Path=%s, Rule=%s]",
                username == null ? "N/A" : username,
                id == null ? "N/A" : id,
                rule == null ? "N/A" : SMS.Rule.toString(rule),
                folder == null? "N/A" : folder.toString()
        );
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public UUID getID() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Byte getRule() {
        return rule;
    }
    
    public void setRule(Byte rule) {
        this.rule = rule;
    }
    
    public Boolean getFolder() {
        return folder;
    }
    
    public void setFolder(Boolean folder) {
        this.folder = folder;
    }
}
