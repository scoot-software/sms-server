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

@ApiModel(description = "User rule")
public class UserRule implements Serializable {
    
    @ApiModelProperty(value = "Username associated with rule", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "smsuser")
    private String username;
    
    @ApiModelProperty(value = "Path", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "/path/related/to/rule")
    private String path;

    @ApiModelProperty(value = "Rule", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "0")
    private Byte rule;

    public UserRule() {};
    
    public UserRule(String username, String path, Byte rule)
    {
        this.username = username;
        this.path = path;
        this.rule = rule;
    }
    
    @Override
    public String toString() {
        return String.format(
                "User[Username=%s, Path=%s, Rule=%s]",
                username == null ? "N/A" : username,
                path == null ? "N/A" : path,
                rule == null ? "N/A" : SMS.Rule.toString(rule)
        );
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Byte getRule() {
        return rule;
    }
    
    public void setRule(Byte rule) {
        this.rule = rule;
    }
}
