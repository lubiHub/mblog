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
package me.qyh.blog.support.file.local;

import static me.qyh.blog.core.file.ImageHelper.JPEG;
import static me.qyh.blog.core.file.ImageHelper.PNG;
import static me.qyh.blog.core.file.ImageHelper.WEBP;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.file.CommonFile;
import me.qyh.blog.core.file.ImageHelper;
import me.qyh.blog.core.file.Resize;
import me.qyh.blog.core.file.ResizeValidator;
import me.qyh.blog.core.file.ThumbnailUrl;
import me.qyh.blog.core.file.ImageHelper.ImageInfo;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.Webs;

/**
 * 本地图片存储，图片访问
 * 
 * @author Administrator
 *
 */
public class ImageResourceStore extends LocalResourceRequestHandlerFileStore {
	private static final Logger IMG_RESOURCE_LOGGER = LoggerFactory.getLogger(ImageResourceStore.class);
	private static final String WEBP_ACCEPT = "image/webp";
	private static final char CONCAT_CHAR = 'X';
	private static final char FORCE_CHAR = '!';

	private static final String NO_WEBP = "nowebp";

	private ResizeValidator resizeValidator;

	@Autowired
	private ImageHelper imageHelper;

	/**
	 * 原图保护
	 */
	private boolean sourceProtected;

	private boolean supportWebp;

	private String thumbAbsPath;
	private Path thumbAbsFolder;

	private Resize smallResize;
	private Resize middleResize;
	private Resize largeResize;

	/**
	 * 最多允许缩放线程数
	 * <p>
	 * 默认为5
	 * </p>
	 */
	private ThreadPoolTaskExecutor executor;

	/**
	 * 防止同时生成相同的缩略图和压缩图
	 */
	private final ConcurrentHashMap<String, CountDownLatch> fileMap = new ConcurrentHashMap<>();

	public ImageResourceStore(String urlPatternPrefix) {
		super(urlPatternPrefix);
	}

	public ImageResourceStore() {
		this("image");
	}

	@Override
	public CommonFile store(String key, MultipartFile mf) throws LogicException {
		Path dest = FileUtils.sub(absFolder, key);
		checkFileStoreable(dest);
		// 先写入临时文件
		String originalFilename = mf.getOriginalFilename();
		Path tmp = FileUtils.appTemp(FileUtils.getFileExtension(originalFilename));
		try {
			Webs.save(mf, tmp);
		} catch (IOException e1) {
			throw new SystemException(e1.getMessage(), e1);
		}
		Path finalFile = tmp;
		try {
			ImageInfo ii = readImage(tmp);
			String extension = ii.getExtension();
			FileUtils.forceMkdir(dest.getParent());
			FileUtils.move(finalFile, dest);
			CommonFile cf = new CommonFile();
			cf.setExtension(extension);
			cf.setSize(mf.getSize());
			cf.setStore(id);
			cf.setOriginalFilename(originalFilename);

			cf.setWidth(ii.getWidth());
			cf.setHeight(ii.getHeight());

			return cf;
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			FileUtils.deleteQuietly(finalFile);
		}
	}

	@Override
	protected Resource findResource(HttpServletRequest request) throws IOException {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new SystemException("Required request attribute '"
					+ HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}
		path = processPath(path);
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			return null;
		}
		return findResource(path, request).orElse(null);
	}

	private void checkFileStoreable(Path dest) throws LogicException {
		if (Files.exists(dest) && !FileUtils.deleteQuietly(dest)) {
			String absPath = dest.toAbsolutePath().toString();
			throw new LogicException("file.store.exists", "文件" + absPath + "已经存在", absPath);
		}
		if (FileUtils.isSub(dest, thumbAbsFolder)) {
			String absPath = dest.toAbsolutePath().toString();
			throw new LogicException("file.inThumb", "文件" + absPath + "不能被存放在缩略图文件夹下", absPath);
		}
	}

	private ImageInfo readImage(Path tmp) throws LogicException {
		try {
			return imageHelper.read(tmp);
		} catch (IOException e) {
			IMG_RESOURCE_LOGGER.debug(e.getMessage(), e);
			throw new LogicException("image.corrupt", "不是正确的图片文件或者图片已经损坏");
		}
	}

	private Optional<Resource> findResource(String path, HttpServletRequest request) {

		// 判断是否是原图
		Optional<Path> optionaLocalFile = super.getFile(path);
		String extension = FileUtils.getFileExtension(path);
		if (optionaLocalFile.isPresent()) {
			// 如果是GIF文件或者没有原图保护，直接输出
			if (ImageHelper.isGIF(extension) || !sourceProtected) {
				return Optional.of(new PathResource(optionaLocalFile.get()));
			}
			return Optional.empty();
		}

		// 原图不存在，从链接中获取缩放信息
		Optional<Resize> optionalResize = getResizeFromPath(path);
		if (!optionalResize.isPresent()) {
			// 如果连接中不包含缩略图信息
			return Optional.empty();
		}
		Resize resize = optionalResize.get();
		String sourcePath = getSourcePathByResizePath(path);
		String ext = FileUtils.getFileExtension(sourcePath);
		// 构造缩略图路径
		Optional<String> optionalThumbPath = getThumbPath(ext, path, request);

		// 如果缩略图路径无法被接受
		if (!optionalThumbPath.isPresent()) {
			return Optional.empty();
		}

		String thumbPath = optionalThumbPath.get();
		// 缩略图是否已经存在
		Path file = findThumbByPath(thumbPath);
		// 缩略图不存在，寻找原图
		if (!Files.exists(file)) {

			Optional<Path> optionalFile = super.getFile(sourcePath);
			// 源文件也不存在
			if (!optionalFile.isPresent()) {
				return Optional.empty();
			}

			// 如果原图存在，进行缩放
			Path local = optionalFile.get();
			// 如果支持文件格式(防止ImageHelper变更)
			if (ImageHelper.isSystemAllowedImage(ext)) {
				try {
					doResize(local, resize, file);
					return Files.exists(file) ? Optional.of(new PathResource(file)) : Optional.empty();
				} catch (Exception e) {
					IMG_RESOURCE_LOGGER.error(e.getMessage(), e);
					// 缩放失败
					return Optional.empty();
				}
			} else {
				// 不支持的文件格式
				// 可能更改了ImageHelper
				// 返回原文件
				return Optional.of(new PathResource(local));
			}

		} else {
			// 直接返回缩略图
			return Optional.of(new PathResource(file));
		}
	}

	@Override
	public boolean delete(String key) {
		boolean flag = super.delete(key);
		if (flag) {
			Path thumbDir = FileUtils.sub(thumbAbsFolder, key);
			if (Files.exists(thumbDir)) {
				flag = FileUtils.deleteQuietly(thumbDir);
			}
		}
		return flag;
	}

	@Override
	public boolean deleteBatch(String key) {
		return delete(key);
	}

	@Override
	public final boolean canStore(MultipartFile multipartFile) {
		String ext = FileUtils.getFileExtension(multipartFile.getOriginalFilename());
		return ImageHelper.isSystemAllowedImage(ext);
	}

	@Override
	public String getUrl(String key) {
		if (sourceProtected) {
			if (ImageHelper.isGIF(FileUtils.getFileExtension(key))) {
				return super.getUrl(key);
			}
			Resize resize = largeResize == null ? (middleResize == null ? smallResize : middleResize) : largeResize;
			return buildResizePath(resize, key);
		} else {
			return super.getUrl(key);
		}
	}

	@Override
	public Optional<ThumbnailUrl> getThumbnailUrl(String key) {
		return Optional.of(new ThumbnailUrl(buildResizePath(smallResize, key), buildResizePath(middleResize, key),
				buildResizePath(largeResize, key)));
	}

	private String buildResizePath(Resize resize, String key) {
		String path = key;
		if (!key.startsWith("/")) {
			path = "/" + key;
		}
		if (resize == null) {
			return getUrl(path);
		}

		return urlPrefix + Validators.cleanPath(generateResizePathFromPath(resize, path));
	}

	@Override
	public void moreAfterPropertiesSet() {

		validateResize(smallResize);
		validateResize(middleResize);
		validateResize(largeResize);

		if (executor == null) {
			throw new SystemException("请提供图片缩放线程池");
		}
		if (thumbAbsPath == null) {
			throw new SystemException("缩略图存储路径不能为null");
		}

		if (sourceProtected && (smallResize == null && middleResize == null && largeResize == null)) {
			throw new SystemException("开启原图保护必须提供默认缩放尺寸");
		}

		List<Resource> resources = new ArrayList<>();
		resources.add(new PathResource(Paths.get(thumbAbsPath)));
		if (sourceProtected) {
			resources.add(new PathResource(Paths.get(absPath)));
		}

		super.setLocations(resources);

		if (!imageHelper.supportWebp()) {
			supportWebp = false;
		}

		thumbAbsFolder = Paths.get(thumbAbsPath);
		FileUtils.forceMkdir(thumbAbsFolder);

		if (resizeValidator == null) {
			resizeValidator = resize -> true;
		}
	}

	private void validateResize(Resize resize) {
		if (resize != null && !resizeValidator.valid(resize)) {
			throw new SystemException("默认缩放尺寸：" + resize + "无法被接受！请调整ResizeUrlParser");
		}
	}

	private Path findThumbByPath(String path) {
		String southPath = getSourcePathByResizePath(path);
		Path thumbDir = FileUtils.sub(thumbAbsFolder, southPath);
		String name = new File(path).getName();
		return FileUtils.sub(thumbDir, name);
	}

	protected void doResize(Path local, Resize resize, Path thumb) throws IOException {
		CountDownLatch cdl = fileMap.get(thumb.normalize().toString());
		if (cdl != null) {
			wait(cdl);
		} else {
			CompletableFuture.runAsync(() -> {
				try {
					executeResize(local, thumb, resize);
				} catch (IOException e) {
					if (Files.exists(local)) {
						throw new SystemException(e.getMessage(), e);
					}
				}
			}, executor).join();
		}
	}

	/*
	 * https://www.qyh.me/space/java/article/graphicsmagick-error-137
	 * 在这种情境下比computIfAbsent(k,Function,null)快
	 */
	private void executeResize(Path local, Path thumb, Resize resize) throws IOException {
		String thumbCanonicalPath = thumb.normalize().toString();

		if (fileMap.putIfAbsent(thumbCanonicalPath, new CountDownLatch(1)) == null) {
			try {
				if (!Files.exists(thumb)) {
					FileUtils.forceMkdir(thumb.getParent());
					imageHelper.resize(resize, local, thumb);
				}
			} finally {
				fileMap.get(thumbCanonicalPath).countDown();
				fileMap.remove(thumbCanonicalPath);
			}
		} else {
			wait(fileMap.get(thumbCanonicalPath));
		}
	}

	private void wait(CountDownLatch cdl) {
		if (cdl != null) {
			try {
				cdl.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new SystemException(e.getMessage(), e);
			}
		}
	}

	protected boolean supportWebp(HttpServletRequest request) {
		if (!supportWebp) {
			return false;
		}
		if (request.getParameter(NO_WEBP) != null) {
			return false;
		}
		String accept = request.getHeader("Accept");
		if (accept != null && accept.indexOf(WEBP_ACCEPT) != -1) {
			return true;
		}
		return false;
	}

	protected String generateResizePathFromPath(Resize resize, String path) {
		if (!resizeValidator.valid(resize)) {
			return path;
		}
		StringBuilder sb = new StringBuilder("/");
		sb.append(getThumname(resize));
		return StringUtils.cleanPath(path + sb.toString());
	}

	protected Optional<Resize> getResizeFromPath(String path) {
		Resize resize = null;
		String baseName = FileUtils.getNameWithoutExtension(path);
		try {
			if (baseName.indexOf(CONCAT_CHAR) != -1) {
				boolean keepRatio = true;
				if (baseName.endsWith(Character.toString(FORCE_CHAR))) {
					keepRatio = false;
					baseName = baseName.substring(0, baseName.length() - 1);
				}
				if (baseName.startsWith(Character.toString(CONCAT_CHAR))) {
					baseName = baseName.substring(1, baseName.length());
					Integer h = Integer.valueOf(baseName);
					resize = new Resize();
					resize.setHeight(h);
					resize.setKeepRatio(keepRatio);
				} else if (baseName.endsWith(Character.toString(CONCAT_CHAR))) {
					baseName = baseName.substring(0, baseName.length() - 1);
					Integer w = Integer.valueOf(baseName);
					resize = new Resize();
					resize.setWidth(w);
					resize.setKeepRatio(keepRatio);
				} else {
					String[] splits = baseName.split(Character.toString(CONCAT_CHAR));
					if (splits.length != 2) {
						return Optional.empty();
					} else {
						Integer w = Integer.valueOf(splits[0]);
						Integer h = Integer.valueOf(splits[1]);
						resize = new Resize(w, h, keepRatio);
					}
				}
			} else {
				resize = new Resize(Integer.valueOf(baseName));
			}
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
		return resizeValidator.valid(resize) ? Optional.of(resize) : Optional.empty();
	}

	private String getThumname(Resize resize) {
		StringBuilder sb = new StringBuilder();
		if (resize.getSize() != null) {
			sb.append(resize.getSize());
		} else {
			sb.append((resize.getWidth() <= 0) ? "" : resize.getWidth());
			sb.append(CONCAT_CHAR);
			sb.append(resize.getHeight() <= 0 ? "" : resize.getHeight());
			sb.append(resize.isKeepRatio() ? "" : FORCE_CHAR);
		}
		return sb.toString();
	}

	protected String getSourcePathByResizePath(String path) {
		String sourcePath = path;
		int idOf = path.lastIndexOf('/');
		if (idOf != -1) {
			sourcePath = path.substring(0, path.lastIndexOf('/'));
		}
		return FileUtils.cleanPath(sourcePath);
	}

	/**
	 * 获取缩略图格式
	 * 
	 * @param sourceExt
	 * @param ext
	 *            访问连接后缀
	 * @param request
	 *            请求
	 * @return
	 */
	private Optional<String> getThumbPath(String sourceExt, String path, HttpServletRequest request) {
		boolean supportWebp = supportWebp(request);
		String ext = FileUtils.getFileExtension(path);
		boolean extEmpty = ext.trim().isEmpty();
		if (extEmpty) {
			return Optional.of(path + "." + (supportWebp ? WEBP : JPEG));
		} else {
			// 如果为png并且原图可能为透明
			if (ImageHelper.isPNG(ext) && ImageHelper.maybeTransparentBg(sourceExt)) {
				String basePath = path.substring(0, path.length() - ext.length() - 1);
				return Optional.of(basePath + "." + PNG);
			}
		}
		return Optional.empty();
	}

	public void setThumbAbsPath(String thumbAbsPath) {
		this.thumbAbsPath = thumbAbsPath;
	}

	public void setImageHelper(ImageHelper imageHelper) {
		this.imageHelper = imageHelper;
	}

	public void setSourceProtected(boolean sourceProtected) {
		this.sourceProtected = sourceProtected;
	}

	public void setResizeValidator(ResizeValidator resizeValidator) {
		this.resizeValidator = resizeValidator;
	}

	public void setSupportWebp(boolean supportWebp) {
		this.supportWebp = supportWebp;
	}

	public void setSmallResize(Resize smallResize) {
		this.smallResize = smallResize;
	}

	public void setMiddleResize(Resize middleResize) {
		this.middleResize = middleResize;
	}

	public void setLargeResize(Resize largeResize) {
		this.largeResize = largeResize;
	}

	public void setExecutor(ThreadPoolTaskExecutor executor) {
		this.executor = executor;
	}
}
