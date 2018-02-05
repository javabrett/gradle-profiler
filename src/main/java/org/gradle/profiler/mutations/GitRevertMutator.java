package org.gradle.profiler.mutations;

import com.typesafe.config.Config;
import org.gradle.profiler.BuildMutator;
import org.gradle.profiler.CommandExec;
import org.gradle.profiler.ConfigUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GitRevertMutator implements BuildMutator {

	private final File projectDir;
	private final List<String> commits;

	public GitRevertMutator(File projectDir, List<String> commits) {
		this.projectDir = projectDir;
		this.commits = commits;
	}

	@Override
	public void beforeScenario() {
		resetGit();
	}

	@Override
	public void beforeBuild() {
        revertCommits();
	}

	@Override
	public void afterBuild(Throwable error) {
		if (error == null) {
			resetGit();
		} else {
			System.out.println("> Not resetting Git because of error during build");
		}
	}

    private void resetGit() {
        System.out.println("> Resetting Git hard");
        new CommandExec().inDir(projectDir).run("git", "reset", "--hard", "HEAD");
    }

    private void revertCommits() {
        System.out.println("> Reverting Git commit(s) " + commits.stream().collect(Collectors.joining(", ")));
        List<String> commandLine = new ArrayList<>();
        commandLine.addAll(Arrays.asList("git", "revert", "--no-commit"));
        commandLine.addAll(commits);
        new CommandExec().inDir(projectDir).run(commandLine);
    }

    public static class Configurator implements BuildMutatorConfigurator {
		@Override
		public Supplier<BuildMutator> configure(Config scenario, String scenarioName, File projectDir, String key) {
			List<String> commits = ConfigUtil.strings(scenario, key, Collections.emptyList());
			if (commits.isEmpty()) {
				throw new IllegalArgumentException("No commits specified for git-revert");
			}
			return () -> new GitRevertMutator(projectDir, commits);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + commits.stream().collect(Collectors.joining(", ")) + ")";
	}
}
