/*
 * Copyright (c) 2009-2010 Clark & Parsia, LLC. <http://www.clarkparsia.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.clarkparsia.openrdf.query.builder;

/**
 * <p>Interface for anything that supports having a collection of groups or sub-groups.</p>
 *
 * @author Michael Grove
 * @version 0.2.2
 * @since 0.2.2
 */
public interface SupportsGroups<T> {

	/**
	 * Add this group from the query
	 * @param theGroup the group to add
	 * @return this builder
	 */
	public T addGroup(Group theGroup);

	/**
	 * Remove this group from the query
	 * @param theGroup the group to remove
	 * @return this builder
	 */
	public T removeGroup(Group theGroup);
}
