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
package org.sonar.server.startup;

import org.junit.Test;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.technicaldebt.TechnicalDebtModelSynchronizer;
import org.sonar.core.technicaldebt.TechnicalDebtRuleCache;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class RegisterTechnicalDebtModelTest {

  @Test
  public void create_model() throws Exception {
    TechnicalDebtModelSynchronizer manger = mock(TechnicalDebtModelSynchronizer.class);
    RuleFinder ruleFinder = mock(RuleFinder.class);
    RegisterTechnicalDebtModel sqaleDefinition = new RegisterTechnicalDebtModel(manger, ruleFinder, null);

    sqaleDefinition.start();

    verify(manger, times(1)).synchronize(any(ValidationMessages.class), any(TechnicalDebtRuleCache.class));
  }
}
