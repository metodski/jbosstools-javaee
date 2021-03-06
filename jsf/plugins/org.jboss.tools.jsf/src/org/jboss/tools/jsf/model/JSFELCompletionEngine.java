/*******************************************************************************
 * Copyright (c) 2007-2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.jsf.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jboss.tools.common.el.core.ca.AbstractELCompletionEngine;
import org.jboss.tools.common.el.core.model.ELInvocationExpression;
import org.jboss.tools.common.el.core.parser.ELParserFactory;
import org.jboss.tools.common.el.core.parser.ELParserUtil;
import org.jboss.tools.common.el.core.resolver.ELContext;
import org.jboss.tools.common.el.core.resolver.IVariable;
import org.jboss.tools.common.el.core.resolver.TypeInfoCollector;
import org.jboss.tools.common.el.core.resolver.TypeInfoCollector.MemberInfo;
import org.jboss.tools.common.model.project.IModelNature;
import org.jboss.tools.common.model.util.EclipseResourceUtil;
import org.jboss.tools.jsf.JSFModelPlugin;
import org.jboss.tools.jsf.model.pv.JSFPromptingProvider;

/**
 * Utility class used to collect info for EL
 * 
 * @author Viacheslav Kabanovich
 */
public class JSFELCompletionEngine extends AbstractELCompletionEngine<JSFELCompletionEngine.IJSFVariable> {

	private static final ImageDescriptor JSF_EL_PROPOSAL_IMAGE = JSFModelPlugin.getDefault().getImageDescriptorFromRegistry(JSFModelPlugin.CA_JSF_EL_IMAGE_PATH);
	private static ELParserFactory factory = ELParserUtil.getJbossFactory();

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.common.el.core.ca.AbstractELCompletionEngine#getELProposalImageForMember(org.jboss.tools.common.el.core.resolver.TypeInfoCollector.MemberInfo)
	 */
	@Override
	public ImageDescriptor getELProposalImageForMember(MemberInfo memberInfo) {
		return JSF_EL_PROPOSAL_IMAGE;
	}

	public JSFELCompletionEngine() {}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.jst.web.kb.el.AbstractELCompletionEngine#log(java.lang.Exception)
	 */
	@Override
	protected void log(Exception e) {
		JSFModelPlugin.getPluginLog().logError(e);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.common.el.core.resolver.ELCompletionEngine#getParserFactory()
	 */
	@Override
	public ELParserFactory getParserFactory() {
		return factory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.jst.web.kb.el.AbstractELCompletionEngine#resolveVariables(org.eclipse.core.resources.IFile, org.jboss.tools.common.el.core.model.ELInvocationExpression, boolean, boolean)
	 */
	@Override
	public List<IJSFVariable> resolveVariables(IFile file, ELContext context, ELInvocationExpression expr, boolean isFinal, boolean onlyEqualNames, int offset) {
		IModelNature project = EclipseResourceUtil.getModelNature(file.getProject());
		
		return resolveVariables(file, context, project, expr, isFinal, onlyEqualNames, offset);
	}

	/**
	 * 
	 * @param project
	 * @param expr
	 * @param isFinal
	 * @param onlyEqualNames
	 * @return
	 */
	private List<IJSFVariable> resolveVariables(IFile file, ELContext context, IModelNature project, ELInvocationExpression expr, boolean isFinal, boolean onlyEqualNames, int offset) {
		List<IJSFVariable> resolvedVars = EMPTY_VARIABLES_LIST;
		
		if (project == null)
			return EMPTY_VARIABLES_LIST; 
		
		String varName = expr.toString();

		if (varName != null) {
			resolvedVars = resolveVariables(project, context, varName, onlyEqualNames, offset);
		}
		if (resolvedVars != null && !resolvedVars.isEmpty()) {
			if(isFinal) {
				return resolvedVars;
			}
			List<IJSFVariable> newResolvedVars = new ArrayList<IJSFVariable>();
			for (IJSFVariable var : resolvedVars) {
				// Do filter by equals (name)
				// In case of the last pass - do not filter by startsWith(name) instead of equals
				if (varName.equals(var.getName())) {
					newResolvedVars.add(var);
				}
			}
			return newResolvedVars;
		} else if (varName != null && (varName.startsWith("\"") || varName.startsWith("'")) && (varName.endsWith("\"") || varName.endsWith("'"))) {
			IJavaProject jp = EclipseResourceUtil.getJavaProject(file.getProject());
			if(jp!=null) {
				try {
					IType type = jp.findType("java.lang.String");
					if (type != null) {
						IMethod m = type.getMethod("toString", new String[0]);
						if (m != null) {
							IJSFVariable v = new Variable("String", m);
							List<IJSFVariable> newResolvedVars = new ArrayList<IJSFVariable>();
							newResolvedVars.add(v);
							return newResolvedVars;
						}
					}
				} catch (JavaModelException e) {
					JSFModelPlugin.getDefault().logError(e);
				}
			}
		}
		return EMPTY_VARIABLES_LIST; 
	}

	static final JSFPromptingProvider PROVIDER = new JSFPromptingProvider();

	protected List<IJSFVariable> resolveVariables(IModelNature project, ELContext context, String varName, boolean onlyEqualNames, int offset) {
		if(project == null) return null;
		List<IJSFVariable> beans = PROVIDER.getVariables(project.getModel());
		List<IJSFVariable> resolvedVariables = EMPTY_VARIABLES_LIST;
		for (IJSFVariable variable: beans) {
			String n = variable.getName();
			boolean add = (onlyEqualNames) ? (n.equals(varName)) : (n.startsWith(varName));
			if(add) {
				if(resolvedVariables == EMPTY_VARIABLES_LIST) {
					resolvedVariables = new ArrayList<IJSFVariable>();
				}
				resolvedVariables.add(variable);
			}
		}
		return resolvedVariables;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.jst.web.kb.el.AbstractELCompletionEngine#getMemberInfoByVariable(org.jboss.tools.jst.web.kb.el.AbstractELCompletionEngine.IVariable, boolean)
	 */
	@Override
	protected TypeInfoCollector.MemberInfo getMemberInfoByVariable(IJSFVariable var, ELContext context, boolean onlyEqualNames, int offset) {
		return TypeInfoCollector.createMemberInfo(((IJSFVariable)var).getSourceMember());		
	}

	public static interface IJSFVariable extends IVariable {
		public IMember getSourceMember();
	}

	public static class Variable implements IJSFVariable {

		private String name;
		private IMember source;

		public Variable(String name, IMember source) {
			this.name = name;
			this.source = source;
		}

		/*
		 * (non-Javadoc)
		 * @see org.jboss.tools.jsf.model.JSFELCompletionEngine.IJSFVariable#getSourceMember()
		 */
		@Override
		public IMember getSourceMember() {
			return source;
		}

		/*
		 * (non-Javadoc)
		 * @see org.jboss.tools.jst.web.kb.el.AbstractELCompletionEngine.IVariable#getName()
		 */
		@Override
		public String getName() {
			return name;
		}
	}

	@Override
	protected boolean isStaticMethodsCollectingEnabled() {
		return false;
	}
}