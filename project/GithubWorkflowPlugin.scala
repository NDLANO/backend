/*
 * Part of NDLA backend
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

import GithubWorkflowPlugin.autoImport.{ghGenerate, ghGenerateEnable, ghGenerateEnableRelease}
import sbt.*
import sbt.Keys.*

object GithubWorkflowPlugin extends AutoPlugin {
  object autoImport {
    val ghGenerateEnableRelease =
      settingKey[Boolean]("Whether or not to enable the release workflow for the component")
    val ghGenerateEnable = settingKey[Boolean]("Whether or not to enable the release workflow for the component")
    val ghGenerate: TaskKey[Unit] =
      taskKey[Unit]("Generate workflow yay")

  }
  val workflowJavaVersion = "20"

  def getSafeName(name: String): String = name.replaceAll("-", "")

  def pathsFor(name: String): Seq[String] = {
    val safeName = getSafeName(name)
    Seq(
      s"$name/**",
      s"project/$safeName*.scala"
    )
  }

  def getPaths(name: String, deps: Seq[String]): Seq[String] = {
    pathsFor(name) ++
      deps.flatMap(pathsFor) ++
      Seq(
        s"project/Dependencies.scala",
        s"project/Module.scala"
      )
  }

  def getPathsList(name: String, deps: Seq[String]): String = {
    val paths = getPaths(name, deps)
    "\n" + paths
      .map(x => s"      - $x")
      .mkString("\n")
  }

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    ghGenerateEnable        := false,
    ghGenerateEnableRelease := false,
    ghGenerate := {
      if (ghGenerateEnable.value) {
        val appName = name.value

        val depNames            = projectDependencies.value.map(_.name)
        val ciYaml              = ci_workflow_yaml(appName, depNames)
        val ciWriteTarget: File = file(s".github/workflows/${appName}_ci.yml")
        IO.write(ciWriteTarget, ciYaml)

        if (ghGenerateEnableRelease.value) {
          val releaseYaml              = release_workflow_yaml(appName, depNames)
          val releaseWriteTarget: File = file(s".github/workflows/${appName}_release.yml")
          IO.write(releaseWriteTarget, releaseYaml)
        }
      }
    }
  )

  override def trigger: PluginTrigger                    = AllRequirements
  override def projectConfigurations: Seq[Configuration] = super.projectConfigurations

  val doNotEditHeader: String =
    s"""# AUTOGENERATED BY: project/GithubWorkflowPlugin.scala (sbt `ghGenerate`)
       |# DO NOT EDIT MANUALLY.""".stripMargin

  def release_workflow_yaml(name: String, depNames: Seq[String]): String = {
    val safeName = getSafeName(name)
    val paths    = getPathsList(name, depNames)
    s"""$doNotEditHeader
       |name: 'Release: $name'
       |on:
       |  workflow_dispatch:
       |    inputs: { }
       |  push:
       |    branches:
       |      - master
       |    paths:$paths
       |env:
       |  AWS_ACCESS_KEY_ID: $${{ secrets.CI_AWS_CLIENT_ID }}
       |  AWS_DEFAULT_REGION: eu-west-1
       |  AWS_SECRET_ACCESS_KEY: $${{ secrets.CI_AWS_CLIENT_SECRET }}
       |  NDLA_AWS_ECR_REPO: $${{ secrets.NDLA_AWS_ECR_REPO }}
       |  CI_RELEASE_ROLE: $${{ secrets.CI_RELEASE_ROLE }}
       |  CI_GITHUB_TOKEN: $${{ secrets.CI_GITHUB_TOKEN }}
       |  DOCKER_HUB_PASSWORD: $${{ secrets.DOCKER_HUB_PASSWORD }}
       |  DOCKER_HUB_USERNAME: $${{ secrets.DOCKER_HUB_USERNAME }}
       |  NDLA_RELEASES: $${{ secrets.NDLA_RELEASES }}
       |  NDLA_ENVIRONMENT: local
       |  NDLA_HOME: $${{ github.workspace }}/ndla
       |  NDLA_DEPLOY: $${{ github.workspace }}/ndla/deploy
       |  COMPONENT: $name
       |  GPG_KEY: $${{ secrets.DEPLOY_BLACKBOX_GPG_KEY_B64 }}
       |jobs:
       |  release:
       |    name: Release and push to registry
       |    runs-on: ubuntu-latest
       |    steps:
       |      - uses: actions/checkout@v4
       |        with:
       |          path: ndla/$${{ github.event.repository.name }}
       |      - uses: actions/checkout@v4
       |        with:
       |          repository: NDLANO/deploy
       |          token: $${{ secrets.CI_GITHUB_TOKEN }}
       |          path: ndla/deploy
       |      - uses: actions/setup-python@v4
       |        with:
       |          python-version: $${{ vars.PYTHON_VERSION }}
       |      - uses: abatilo/actions-poetry@v2
       |        with:
       |          poetry-version: $${{ vars.POETRY_VERSION }}
       |      - uses: actions/setup-java@v3
       |        with:
       |          distribution: temurin
       |          java-version: '$workflowJavaVersion'
       |      - uses: hashicorp/setup-terraform@v3
       |        with:
       |          terraform_version: $${{ vars.TERRAFORM_VERSION }}
       |      - name: Setup ~/bin directory
       |        run: |
       |          mkdir -p /home/runner/bin
       |          echo "/home/runner/bin" >> $$GITHUB_PATH
       |      - name: Login to ECR repo
       |        run: RES=$$(aws sts assume-role --role-arn $$CI_RELEASE_ROLE --role-session-name
       |          github-actions-ecr-login) AWS_ACCESS_KEY_ID=$$(echo $$RES | jq -r .Credentials.AccessKeyId)
       |          AWS_SECRET_ACCESS_KEY=$$(echo $$RES | jq -r .Credentials.SecretAccessKey) AWS_SESSION_TOKEN=$$(echo
       |          $$RES | jq -r .Credentials.SessionToken) aws ecr get-login-password --region
       |          eu-central-1 | docker login --username AWS --password-stdin $$NDLA_AWS_ECR_REPO
       |      - name: Login to dockerhub
       |        run: echo $$DOCKER_HUB_PASSWORD | docker login --username $$DOCKER_HUB_USERNAME
       |          --password-stdin
       |      - name: Cache pip
       |        uses: actions/cache@v3
       |        with:
       |          path: ndla/deploy/.venv
       |          key: $${{ runner.os }}-pip-$${{ hashFiles('ndla/deploy/poetry.lock')
       |            }}
       |          restore-keys: |
       |            $${{ runner.os }}-pip-
       |            $${{ runner.os }}-
       |      - name: Install python dependencies
       |        run: |
       |          # Setup the virtualenv in the repo to make caching of dependencies easier
       |          poetry config virtualenvs.create true --local
       |          poetry config virtualenvs.in-project true --local
       |
       |          # Install the deps!
       |          poetry --directory $$NDLA_DEPLOY install
       |      - name: Download blackbox
       |        uses: actions/checkout@v3
       |        with:
       |          repository: StackExchange/blackbox
       |          path: blackbox
       |      - name: Install Blackbox and key
       |        run: |
       |          # Move binaries to path
       |          sudo mv blackbox/bin/* /home/runner/bin/
       |          echo -n "$$GPG_KEY" | base64 --decode | gpg --import
       |      - name: Install kubectl
       |        run: |
       |          curl -L https://storage.googleapis.com/kubernetes-release/release/v1.21.11/bin/linux/amd64/kubectl > kubectl
       |          sudo mv kubectl /home/runner/bin/kubectl
       |          sudo chmod +x /home/runner/bin/kubectl
       |          mkdir -p ~/.kube
       |      - name: Install aws-iam-authenticator
       |        run: |
       |          sudo curl -L https://amazon-eks.s3-us-west-2.amazonaws.com/1.12.7/2019-03-27/bin/linux/amd64/aws-iam-authenticator > aws-iam-authenticator
       |          sudo mv aws-iam-authenticator /home/runner/bin/aws-iam-authenticator
       |          sudo chmod +x /home/runner/bin/aws-iam-authenticator
       |      - name: Install helm /w push-plugin
       |        run: |
       |          curl -L https://get.helm.sh/helm-v3.11.3-linux-amd64.tar.gz > /tmp/helm.tar.gz
       |          tar xvzf /tmp/helm.tar.gz -C /tmp/
       |          sudo mv /tmp/linux-amd64/helm /home/runner/bin/
       |          sudo chmod +x /home/runner/bin/helm
       |      - name: Build kubernetes config
       |        run: poetry -C $$NDLA_DEPLOY run ndla env kubeconfig test
       |      - name: Do release
       |        shell: bash
       |        run: poetry -C $$NDLA_DEPLOY run ndla release $$COMPONENT --update-chart
       |""".stripMargin

  }

  def ci_workflow_yaml(name: String, depNames: Seq[String]): String = {
    val safeName = getSafeName(name)
    val paths    = getPathsList(name, depNames)
    s"""$doNotEditHeader
      |name: 'CI: $name'
      |on:
      |  workflow_dispatch:
      |    inputs: { }
      |  push:
      |    paths:$paths
      |  pull_request:
      |    paths:$paths
      |env:
      |  AWS_ACCESS_KEY_ID: $${{ secrets.CI_AWS_CLIENT_ID }}
      |  AWS_DEFAULT_REGION: eu-west-1
      |  AWS_SECRET_ACCESS_KEY: $${{ secrets.CI_AWS_CLIENT_SECRET }}
      |  NDLA_AWS_ECR_REPO: $${{ secrets.NDLA_AWS_ECR_REPO }}
      |  CI_RELEASE_ROLE: $${{ secrets.CI_RELEASE_ROLE }}
      |  CI_GITHUB_TOKEN: $${{ secrets.CI_GITHUB_TOKEN }}
      |  DOCKER_HUB_PASSWORD: $${{ secrets.DOCKER_HUB_PASSWORD }}
      |  DOCKER_HUB_USERNAME: $${{ secrets.DOCKER_HUB_USERNAME }}
      |  NDLA_RELEASES: $${{ secrets.NDLA_RELEASES }}
      |  COMPONENT: $name
      |jobs:
      |  unit_tests:
      |    name: Unit tests
      |    runs-on: ubuntu-latest
      |    steps:
      |      - uses: actions/checkout@v3
      |      - uses: coursier/cache-action@v6
      |      - uses: actions/setup-java@v3
      |        with:
      |          distribution: temurin
      |          java-version: '$workflowJavaVersion'
      |      - uses: sbt/setup-sbt@v1
      |      - name: Login to ECR repo
      |        run: RES=$$(aws sts assume-role --role-arn $$CI_RELEASE_ROLE --role-session-name
      |          github-actions-ecr-login) AWS_ACCESS_KEY_ID=$$(echo $$RES | jq -r .Credentials.AccessKeyId)
      |          AWS_SECRET_ACCESS_KEY=$$(echo $$RES | jq -r .Credentials.SecretAccessKey) AWS_SESSION_TOKEN=$$(echo
      |          $$RES | jq -r .Credentials.SessionToken) aws ecr get-login-password --region
      |          eu-central-1 | docker login --username AWS --password-stdin $$NDLA_AWS_ECR_REPO
      |      - name: Login to dockerhub
      |        run: echo $$DOCKER_HUB_PASSWORD | docker login --username $$DOCKER_HUB_USERNAME
      |          --password-stdin
      |      - name: Unit tests
      |        run: sbt $name/test
      |""".stripMargin
  }

}
