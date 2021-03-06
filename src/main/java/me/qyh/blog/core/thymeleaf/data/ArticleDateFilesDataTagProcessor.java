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
package me.qyh.blog.core.thymeleaf.data;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.bean.ArticleDateFiles;
import me.qyh.blog.core.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.ArticleService;

public class ArticleDateFilesDataTagProcessor extends DataTagProcessor<ArticleDateFiles> {

	@Autowired
	private ArticleService articleService;

	private static final String MODE = "mode";

	public ArticleDateFilesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected ArticleDateFiles query(Attributes attributes) throws LogicException {
		ArticleDateFileMode mode = getMode(attributes);
		return articleService.queryArticleDateFiles(mode);
	}

	private ArticleDateFileMode getMode(Attributes attributes) {
		ArticleDateFileMode mode = ArticleDateFileMode.YM;
		String v = attributes.getOrDefault(MODE, ArticleDateFileMode.YM.name());
		try {
			mode = ArticleDateFileMode.valueOf(v);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}
		return mode;
	}

}
