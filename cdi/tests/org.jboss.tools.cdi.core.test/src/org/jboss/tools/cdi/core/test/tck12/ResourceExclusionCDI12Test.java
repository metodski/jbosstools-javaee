/******************************************************************************* 
 * Copyright (c) 2014 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdi.core.test.tck12;

import org.jboss.tools.cdi.core.test.tck.ITCKProjectNameProvider;
import org.jboss.tools.cdi.core.test.tck.ResourceExclusionTest;

/**
 * @author Alexey Kazakov
 */
public class ResourceExclusionCDI12Test extends ResourceExclusionTest {

	@Override
	public ITCKProjectNameProvider getProjectNameProvider() {
		return new TCK12ProjectNameProvider();
	}
}