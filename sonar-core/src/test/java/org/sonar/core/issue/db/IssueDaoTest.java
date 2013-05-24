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

package org.sonar.core.issue.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;


public class IssueDaoTest extends AbstractDaoTestCase {

  IssueDao dao;

  @Before
  public void createDao() {
    dao = new IssueDao(getMyBatis());
  }

  @Test
  public void should_select_by_key() {
    setupData("shared", "should_select_by_key");

    IssueDto issue = dao.selectByKey("ABCDE");
    assertThat(issue.getKee()).isEqualTo("ABCDE");
    assertThat(issue.getId()).isEqualTo(100L);
    assertThat(issue.getResourceId()).isEqualTo(401);
    assertThat(issue.getProjectId()).isEqualTo(399);
    assertThat(issue.getRuleId()).isEqualTo(500);
    assertThat(issue.getSeverity()).isEqualTo("BLOCKER");
    assertThat(issue.isManualSeverity()).isFalse();
    assertThat(issue.getMessage()).isNull();
    assertThat(issue.getLine()).isEqualTo(200);
    assertThat(issue.getEffortToFix()).isEqualTo(4.2);
    assertThat(issue.getStatus()).isEqualTo("OPEN");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getChecksum()).isEqualTo("XXX");
    assertThat(issue.getAuthorLogin()).isEqualTo("karadoc");
    assertThat(issue.getReporter()).isEqualTo("arthur");
    assertThat(issue.getAssignee()).isEqualTo("perceval");
    assertThat(issue.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(issue.getIssueCreationDate()).isNotNull();
    assertThat(issue.getIssueUpdateDate()).isNotNull();
    assertThat(issue.getIssueCloseDate()).isNotNull();
    assertThat(issue.getCreatedAt()).isNotNull();
    assertThat(issue.getUpdatedAt()).isNotNull();
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
    assertThat(issue.getProjectKey()).isEqualTo("struts");
  }

  @Test
  public void should_select_by_rules() {
    setupData("shared", "should_select_by_rules");

    IssueQuery query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "AvoidCycle"))).build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).hasSize(2);

    query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("squid", "NullRef"))).build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).hasSize(3);

    query = IssueQuery.builder().rules(newArrayList(RuleKey.of("squid", "Other"))).build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).isEmpty();
  }

  @Test
  public void should_select_by_date_creation() {
    setupData("shared", "should_select_by_date_creation");

    IssueQuery query = IssueQuery.builder().createdAfter(DateUtils.parseDate("2013-04-15")).build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).hasSize(1);

    query = IssueQuery.builder().createdBefore(DateUtils.parseDate("2013-04-17")).build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).hasSize(2);
  }

  @Test
  public void should_select_by_component_root() {
    setupData("shared", "should_select_by_component_root");

    IssueQuery query = IssueQuery.builder().componentRoots(newArrayList("struts")).build();
    List<IssueDto> issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).getId()).isEqualTo(100);
    assertThat(issues.get(1).getId()).isEqualTo(101);

    query = IssueQuery.builder().componentRoots(newArrayList("struts-core")).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).getId()).isEqualTo(100);
    assertThat(issues.get(1).getId()).isEqualTo(101);

    query = IssueQuery.builder().componentRoots(newArrayList("Filter.java")).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getId()).isEqualTo(101);

    query = IssueQuery.builder().componentRoots(newArrayList("not-found")).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).isEmpty();
  }

  @Test
  public void should_select_by_assigned() {
    setupData("shared", "should_select_by_assigned");

    IssueQuery query = IssueQuery.builder().assigned(true).build();
    List<IssueDto> issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(2);

    query = IssueQuery.builder().assigned(false).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(1);

    query = IssueQuery.builder().assigned(null).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(3);
  }

  @Test
  public void should_select_by_planned() {
    setupData("shared", "should_select_by_planned");

    IssueQuery query = IssueQuery.builder().planned(true).build();
    List<IssueDto> issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(2);

    query = IssueQuery.builder().planned(false).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(1);

    query = IssueQuery.builder().planned(null).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(3);
  }

  @Test
  public void should_select_by_resolved() {
    setupData("shared", "should_select_by_resolved");

    IssueQuery query = IssueQuery.builder().resolved(true).build();
    List<IssueDto> issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(2);

    query = IssueQuery.builder().resolved(false).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(1);

    query = IssueQuery.builder().resolved(null).build();
    issues = newArrayList(dao.selectIssueAndProjectIds(query, newArrayList(399)));
    assertThat(issues).hasSize(3);
  }

  @Test
  public void should_select_by_action_plans() {
    setupData("shared", "should_select_by_action_plans");

    IssueQuery query = IssueQuery.builder().actionPlans(newArrayList("ABC")).build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).hasSize(2);

    query = IssueQuery.builder().actionPlans(newArrayList("ABC", "DEF")).build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).hasSize(3);

    query = IssueQuery.builder().actionPlans(newArrayList("<Unkown>")).build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).isEmpty();
  }

  @Test
  public void should_select_all() {
    setupData("shared", "should_select_all");

    IssueQuery query = IssueQuery.builder().build();
    assertThat(dao.selectIssueAndProjectIds(query, newArrayList(399))).hasSize(3);
  }

  @Test
  public void should_select_issue_and_project_ids() {
    setupData("shared", "should_select_issue_and_project_ids");

    IssueQuery query = IssueQuery.builder().build();
    List<IssueDto> results = dao.selectIssueAndProjectIds(query, newArrayList(399));
    assertThat(results).hasSize(3);

    IssueDto issueDto = results.get(0);
    assertThat(issueDto.getId()).isNotNull();
    assertThat(issueDto.getProjectId()).isNotNull();

    results = dao.selectIssueAndProjectIds(query, Collections.<Integer>emptyList());
    assertThat(results).isEmpty();

    results = dao.selectIssueAndProjectIds(query, newArrayList(399), 2);
    assertThat(results).hasSize(2);
  }

  @Test
  public void should_select_issue_and_project_ids_with_sort_column() {
    setupData("shared", "should_select_issue_and_project_ids");

    IssueQuery query = IssueQuery.builder().sort(IssueQuery.Sort.ASSIGNEE).build();
    List<IssueDto> results = dao.selectIssueAndProjectIds(query, newArrayList(399));
    assertThat(results.get(0).getAssignee()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.Sort.SEVERITY).build();
    results = dao.selectIssueAndProjectIds(query, newArrayList(399));
    assertThat(results.get(0).getSeverity()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.Sort.STATUS).build();
    results = dao.selectIssueAndProjectIds(query, newArrayList(399));
    assertThat(results.get(0).getStatus()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.Sort.CREATION_DATE).build();
    results = dao.selectIssueAndProjectIds(query, newArrayList(399));
    assertThat(results.get(0).getIssueCreationDate()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.Sort.UPDATE_DATE).build();
    results = dao.selectIssueAndProjectIds(query, newArrayList(399));
    assertThat(results.get(0).getIssueUpdateDate()).isNotNull();

    query = IssueQuery.builder().sort(IssueQuery.Sort.CLOSE_DATE).build();
    results = dao.selectIssueAndProjectIds(query, newArrayList(399));
    assertThat(results.get(0).getIssueCloseDate()).isNotNull();
  }

  @Test
  public void should_select_open_issues() {
    setupData("shared", "should_select_open_issues");

    List<IssueDto> dtos = dao.selectNonClosedIssuesByRootComponent(400);
    assertThat(dtos).hasSize(2);

    IssueDto issue = dtos.get(0);
    assertThat(issue.getRuleRepo()).isNotNull();
    assertThat(issue.getRule()).isNotNull();
    assertThat(issue.getComponentKey()).isNotNull();
  }

  @Test
  public void should_select_by_ids() {
    setupData("shared", "should_select_by_ids");

    List<IssueDto> results = newArrayList(dao.selectByIds(newArrayList(100l, 101l, 102l)));
    assertThat(results).hasSize(3);
  }
}
