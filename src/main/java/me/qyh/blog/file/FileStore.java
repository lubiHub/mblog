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
package me.qyh.blog.file;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;

public interface FileStore {

	/**
	 * 存储文件
	 * 
	 * @param multipartFile
	 * @return
	 * @throws LogicException
	 *             存储异常
	 */
	CommonFile store(String key, MultipartFile multipartFile) throws LogicException, IOException;

	/**
	 * 存储器ID
	 * 
	 * @return
	 */
	int id();

	/**
	 * 删除物理文件
	 * 
	 * @param key
	 * @return true:删除成功|文件不存在，无需删除 false :删除失败(可能占用中)
	 */
	boolean delete(String key);

	/**
	 * 删除文件夹下物理文件
	 * 
	 * @param key
	 * @return true:如果文件夹不存在或者全部文件删除成功
	 */
	boolean deleteBatch(String key);

	/**
	 * 获取文件的访问路径
	 * 
	 * @param key
	 * @return
	 */
	String getUrl(String key);

	/**
	 * 获取下载路径
	 * 
	 * @param cf
	 * @return
	 */
	String getDownloadUrl(String key);

	/**
	 * 获取缩略图路径
	 * 
	 * @param cf
	 * @return
	 */
	ThumbnailUrl getThumbnailUrl(String key);

}
