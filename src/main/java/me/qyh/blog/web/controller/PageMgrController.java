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
package me.qyh.blog.web.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.pageparam.SpaceQueryParam;
import me.qyh.blog.core.pageparam.TemplatePageQueryParam;
import me.qyh.blog.core.service.SpaceService;
import me.qyh.blog.core.thymeleaf.TemplateService;
import me.qyh.blog.core.thymeleaf.template.Page;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.blog.web.controller.form.TemplatePageQueryParamValidator;

@Controller
@RequestMapping("mgr/template/page")
public class PageMgrController extends BaseMgrController {

	@Autowired
	private TemplatePageQueryParamValidator pageParamValidator;
	@Autowired
	private TemplateService templateService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private PageValidator pageValidator;
	@Autowired
	private UrlHelper urlHelper;

	@InitBinder(value = "page")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@InitBinder(value = "templatePageQueryParam")
	protected void initTemplatePageQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(pageParamValidator);
	}

	@GetMapping("index")
	public String index(@Validated TemplatePageQueryParam templatePageQueryParam, BindingResult result, Model model) {
		if (result.hasErrors()) {
			templatePageQueryParam = new TemplatePageQueryParam();
			templatePageQueryParam.setCurrentPage(1);
		}
		model.addAttribute("spaces", spaceService.querySpace(new SpaceQueryParam()));
		model.addAttribute("result", templateService.queryPage(templatePageQueryParam));
		return "mgr/template/page";
	}

	@PostMapping("build")
	@ResponseBody
	public JsonResult build(@RequestBody @Validated Page page) throws LogicException {
		templateService.buildTpl(page);
		return new JsonResult(true, new Message("page.user.build.success", "保存成功"));
	}

	@GetMapping(value = "new")
	public String build(Model model) {
		model.addAttribute("page", new Page());
		SpaceQueryParam param = new SpaceQueryParam();
		model.addAttribute("spaces", spaceService.querySpace(param));
		return "mgr/template/page_build";
	}

	@GetMapping(value = "update")
	public String update(@RequestParam("id") Integer id, Model model, RedirectAttributes ra) {
		Optional<Page> optional = templateService.queryPage(id);
		if (!optional.isPresent()) {
			ra.addFlashAttribute(ERROR, new Message("page.user.notExists", "自定义页面不存在"));
			return "redirect:/mgr/template/page";
		}
		Page page = optional.get();
		model.addAttribute("page", page);
		SpaceQueryParam param = new SpaceQueryParam();
		model.addAttribute("spaces", spaceService.querySpace(param));
		return "mgr/template/page_build";
	}

	@PostMapping("preview")
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated Page page) throws LogicException {
		Space space = page.getSpace();
		if (space != null) {
			space = spaceService.getSpace(space.getId()).orElse(null);
		}
		page.setSpace(space);

		templateService.getPreviewService().registerPreview(page.getTemplatePath(), page);
		return new JsonResult(true, new PreviewUrl(urlHelper.getUrls().getUrl(page)));
	}

	@PostMapping("delete")
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		templateService.deletePage(id);
		return new JsonResult(true, new Message("page.user.delete.success", "删除成功"));
	}
}
