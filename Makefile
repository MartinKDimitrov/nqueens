.PHONY: check check-device

# Everything a commit must pass. The pre-commit hook and CI both run this and nothing else.
check:
	./gradlew check buildHealth

# What a workstation cannot answer: whether the platform's decoder takes the sounds, and what the
# system draws around the app. Needs a device or an emulator, so it is not part of `check`.
#
# Only `:app` has device tests, and it is named rather than swept for: every library module has
# this task whether or not it has a test to run, and an instrumentation APK with nothing in it does
# not report nothing — it crashes, and the gate goes red for a module nobody asked a question.
check-device:
	./gradlew :app:connectedDebugAndroidTest
