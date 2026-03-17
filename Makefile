.PHONY: build
build:
	@echo "🏗️ Building..."
	@./gradlew checkLegacyAbi build koverVerify koverXmlReport koverHtmlReport koverLog

.PHONY: apidump
apidump:
	@echo "🪏 API dump..."
	@./gradlew updateLegacyAbi


.PHONY: clean
clean:
	@echo "🧽 Cleaning..."
	@./gradlew clean
	@rm -rf kotlin-js-store

.PHONY: test
test:
	./gradlew --rerun-tasks check

.PHONY: apidocs
apidocs:
	@echo "📑 Running Dokka.."
	@./gradlew dokkaGenerate

.PHONY: knit
knit:
	@echo "🪡🧶 Running Knit..."
	@./gradlew :mokksy:clean
	@./gradlew knit :mokksy:jvmTest --rerun-tasks
	@echo "✅ Knit completed!"

.PHONY: lint
lint:
	@./gradlew detekt

# https://docs.openrewrite.org/recipes/maven/bestpractices
.PHONY: format
format:
	@./gradlew rewriteRun detekt --auto-correct

.PHONY: all
all: format lint build

.PHONY: pom
pom:
	@./gradlew generatePomFileForKotlinMultiplatformPublication

.PHONY: publish
publish:
	rm -rf ~/.m2/repository/me/kpavlov/mokksy
	./gradlew --rerun-tasks clean build check sourcesJar publishToMavenLocal
	echo "Publishing 📢"
	## https://vanniktech.github.io/gradle-maven-publish-plugin/central/#configuring-maven-central
	# ./gradlew publishToMavenCentral \
	# -PmavenCentralUsername="$SONATYPE_USERNAME" \
  # -PmavenCentralPassword="$SONATYPE_PASSWORD"
