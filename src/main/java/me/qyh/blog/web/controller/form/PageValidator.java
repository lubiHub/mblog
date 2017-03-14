/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.ui.TemplateUtils;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Validators;

@Component
public class PageValidator implements Validator {

	public static final int PAGE_TPL_MAX_LENGTH = 500000;

	protected static final int PAGE_NAME_MAX_LENGTH = 20;
	protected static final int PAGE_DESCRIPTION_MAX_LENGTH = 500;
	protected static final int PAGE_ALIAS_MAX_LENGTH = 255;

	/**
	 * 最长深度 String.split("/").length
	 */
	private static final int MAX_ALIAS_DEPTH = 10;

	private static final String NO_REGISTRABLE_ALIAS_PATTERN = "^[A-Za-z0-9_-]+$";

	@Override
	public boolean supports(Class<?> clazz) {
		return Page.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Page page = (Page) target;
		String pageTpl = page.getTpl();
		if (Validators.isEmptyOrNull(pageTpl, true)) {
			errors.reject("page.tpl.null", "页面模板不能为空");
			return;
		}

		if (pageTpl.length() > PAGE_TPL_MAX_LENGTH) {
			errors.reject("page.tpl.toolong", new Object[] { PAGE_TPL_MAX_LENGTH },
					"页面模板不能超过" + PAGE_TPL_MAX_LENGTH + "个字符");
			return;
		}

		if (page instanceof LockPage) {
			LockPage lockPage = (LockPage) page;
			if (Validators.isEmptyOrNull(lockPage.getLockType(), true)) {
				errors.reject("page.locktype.empty", "页面锁类型不能为空");
				return;
			}
		}

		if (page instanceof UserPage) {
			UserPage userPage = (UserPage) page;
			String name = userPage.getName();
			if (Validators.isEmptyOrNull(name, true)) {
				errors.reject("page.name.blank", "页面名称不能为空");
				return;
			}
			if (name.length() > PAGE_NAME_MAX_LENGTH) {
				errors.reject("page.name.toolong", new Object[] { PAGE_NAME_MAX_LENGTH },
						"页面名称不能超过" + PAGE_NAME_MAX_LENGTH + "个字符");
				return;
			}
			String description = userPage.getDescription();
			if (description == null) {
				errors.reject("page.description.null", "页面描述不能为空");
				return;
			}
			if (description.length() > PAGE_DESCRIPTION_MAX_LENGTH) {
				errors.reject("page.description.toolong", new Object[] { PAGE_DESCRIPTION_MAX_LENGTH },
						"页面描述不能超过" + PAGE_DESCRIPTION_MAX_LENGTH + "个字符");
				return;
			}
			String alias = userPage.getAlias();
			if (Validators.isEmptyOrNull(alias, true)) {
				alias = "/";
			} else {
				alias = TemplateUtils.cleanUserPageAlias(alias);
			}
			if (alias.isEmpty()) {
				errors.reject("page.alias.blank", "页面别名不能为空");
				return;
			}
			if (alias.length() > PAGE_ALIAS_MAX_LENGTH) {
				errors.reject("page.alias.toolong", new Object[] { PAGE_ALIAS_MAX_LENGTH },
						"页面别名不能超过" + PAGE_ALIAS_MAX_LENGTH + "个字符");
				return;
			}
			if (!validateUserPageAlias(alias)) {
				errors.reject("page.alias.invalid", "页面别名不被允许");
				return;
			}

			userPage.setAlias(alias);

			if (userPage.getAllowComment() == null) {
				errors.reject("page.allowComment", "是否允许评论不能为空");
				return;
			}
		}

	}

	public static boolean validateUserPageAlias(String alias) {

		/**
		 * 这里重复校验了，为了 UserPageController
		 */
		if (alias == null) {
			return false;
		}
		if (alias.length() > PAGE_ALIAS_MAX_LENGTH) {
			return false;
		}

		if (alias.equals("/")) {
			return true;
		}

		if (alias.indexOf('/') == -1) {
			return doValidateUserPageAlias(alias);
		} else {

			String[] aliasArray = alias.split("/");
			if (aliasArray.length > MAX_ALIAS_DEPTH) {
				return false;
			}
			for (String _alias : aliasArray) {
				if (!doValidateUserPageAlias(_alias)) {
					return false;
				}
			}
			return true;
		}
	}

	private static boolean doValidateUserPageAlias(String alias) {
		if (alias.startsWith("{")) {
			if (!alias.endsWith("}")) {
				return false;
			}
			String pattern = alias.substring(1, alias.length() - 1);
			return Validators.isLetter(pattern);
		} else {
			return alias.matches(NO_REGISTRABLE_ALIAS_PATTERN);
		}
	}
}
