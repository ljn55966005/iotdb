/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.mpp.sql.planner.plan;

import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNode;

// TODO: consider whether it is necessary to make PlanFragment as a TreeNode
/** PlanFragment contains a sub-query of distributed query. */
public class PlanFragment {
  private PlanFragmentId id;
  private PlanNode root;

  public PlanFragment(PlanFragmentId id, PlanNode root) {
    this.id = id;
    this.root = root;
  }

  public PlanFragmentId getId() {
    return id;
  }

  public PlanNode getRoot() {
    return root;
  }

  public String toString() {
    return String.format("PlanFragment-%s", getId());
  }
}
