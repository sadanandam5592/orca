/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.igor.tasks;

import com.netflix.spinnaker.kork.artifacts.model.Artifact;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.igor.BuildService;
import com.netflix.spinnaker.orca.igor.model.CIStageDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetBuildArtifactsTask extends RetryableIgorTask<CIStageDefinition> {
  private final BuildService buildService;

  @Override
  protected @Nonnull TaskResult tryExecute(@Nonnull CIStageDefinition stageDefinition) {
    List<Artifact> artifacts =
        buildService.getArtifacts(
            stageDefinition.getBuildNumber(),
            stageDefinition.getPropertyFile(),
            stageDefinition.getMaster(),
            stageDefinition.getJob());
    if (artifacts == null) {
      artifacts = new ArrayList<>();
    }
    stageDefinition.getBuildInfo().getArtifacts().stream()
        .filter(
            artifact ->
                (boolean)
                    Optional.ofNullable(artifact.getMetadata())
                        .orElse(Collections.emptyMap())
                        .getOrDefault("decorated", false))
        .forEach(artifacts::add);
    Map<String, List<Artifact>> outputs = Collections.singletonMap("artifacts", artifacts);
    return TaskResult.builder(ExecutionStatus.SUCCEEDED)
        .context(Collections.emptyMap())
        .outputs(outputs)
        .build();
  }

  @Override
  protected @Nonnull CIStageDefinition mapStage(@Nonnull StageExecution stage) {
    return stage.mapTo(CIStageDefinition.class);
  }
}
