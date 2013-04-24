/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.issue;

import org.json.simple.JSONValue;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import java.util.List;
import java.util.Map;

class IssueParser {
  Issues parseIssues(String json) {
    Issues result = new Issues();
    Map jRoot = (Map) JSONValue.parse(json);
    List<Map> jIssues = (List) jRoot.get("issues");
    for (Map jIssue : jIssues) {
      result.add(new Issue(jIssue));
    }
    Map paging = (Map) jRoot.get("paging");
    result.setPaging(new Paging(paging));

    result.setSecurityExclusions(JsonUtils.getBoolean(jRoot, "securityExclusions"));
    return result;
  }
}