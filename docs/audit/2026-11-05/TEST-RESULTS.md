# 2026-11-05 — 本轮逐用例执行结果

同一用例的重试不重复计数。`共享/桌面JVM`在Linux运行；它包含可在JVM执行的共享测试，不表示Windows实机通过。Android以独立验证包执行，未使用正式应用数据。失败和未完成另见[总报告](FEATURE-TEST-REPORT.md)。

<a id="albumcoverrepositorytest"></a>
### AlbumCoverRepositoryTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/enrich/AlbumCoverRepositoryTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/enrich/AlbumCoverRepositoryTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `catalogRejectsCoversAndLiveVersionsAndPrefersRequestedAlbum` | 通过 |

<a id="androidexportlocationtest"></a>
### AndroidExportLocationTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AndroidExportLocationTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AndroidExportLocationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `directoryUriTargetsTheExportSubfolderAndOtherLocationsAreRejected` | 通过 |
| Android设备 | `folderLaunchUsesAFreshDocumentTaskAndCarriesNoStorageGrant` | 通过 |

<a id="announcementtest"></a>
### AnnouncementTest

源码：[website/tests/test_announcements.py](../../../website/tests/test_announcements.py)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Python/website | `test_checked_in_public_content_matches_release_and_explains_network_boundary` | 通过 |
| Python/website | `test_direct_download_urls_change_with_release_to_avoid_old_cached_installer` | 通过 |
| Python/website | `test_publication_only_updates_announcement_and_does_not_send_mail` | 通过 |
| Python/website | `test_wrong_release_and_invalid_body_preserve_previous_announcement` | 通过 |

<a id="applicationlifecycledevicetest"></a>
### ApplicationLifecycleDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/ApplicationLifecycleDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/ApplicationLifecycleDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `repeatedRecreationAndShutdownKeepTheMainLooperResponsive` | 通过 |

<a id="applicationsessiontest"></a>
### ApplicationSessionTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/ApplicationSessionTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/ApplicationSessionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `cancellationCleanupCanCommitBeforeTheWritersAreClosed` | 通过 |
| 共享/桌面JVM | `cancellingOneQueuedOperationDoesNotStrandAnOrderedBarrier` | 通过 |
| 共享/桌面JVM | `failingTaskIsObservableAndDoesNotCancelOtherApplicationWork` | 通过 |
| 共享/桌面JVM | `oneFailedDrainDoesNotSkipOtherWritersOrNativeCleanup` | 通过 |
| 共享/桌面JVM | `shutdownStopsProducersThenDrainsEveryWriterBeforeReleasingPlatform` | 通过 |

<a id="applicationusecasetest"></a>
### ApplicationUseCaseTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ApplicationUseCaseTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ApplicationUseCaseTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `cancelledEmptyImportDoesNotReloadOrCommitAnOlderUiCatalog` | 通过 |
| 共享/桌面JVM | `importingPackageReloadsCommittedPlaylistsAfterTheWriteBarrier` | 通过 |
| 共享/桌面JVM | `preferenceChangesShareOneCapturedOrderedTransactionStream` | 通过 |

<a id="artworkdecodetest"></a>
### ArtworkDecodeTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/ArtworkDecodeTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/ArtworkDecodeTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `pngAndWebpCoversAreSupportedWithoutKeepingFullResolution` | 通过 |

<a id="assistantmusiclinkstest"></a>
### AssistantMusicLinksTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/AssistantMusicLinksTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/AssistantMusicLinksTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `attachmentsAreBoundedAndQueueKeepsExistingOrder` | 通过 |
| 共享/桌面JVM | `duplicateLinksCollapseButPlainNamesAndExternalLinksNeverBecomePlayable` | 通过 |
| 共享/桌面JVM | `encodedIdsResolveToCurrentLibraryMetadataInReplyOrder` | 通过 |
| 共享/桌面JVM | `staleAndMalformedIdsRemainReadableWithoutInventingTracks` | 通过 |

<a id="assistanttoolsessiontest"></a>
### AssistantToolSessionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/AssistantToolSessionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/AssistantToolSessionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `compactionKeepsJsonAndWholeIdsInsteadOfCuttingArrays` | 通过 |
| 共享/桌面JVM | `failedDuplicateRunsOnceAndStopsWithoutAnotherRequestAfterSixIdleRounds` | 通过 |
| 共享/桌面JVM | `identicalMutationsRunOnceEvenAfterManySuccessfulRoundsAndArgumentReordering` | 通过 |

<a id="audiocontrollerlifecycledevicetest"></a>
### AudioControllerLifecycleDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AudioControllerLifecycleDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AudioControllerLifecycleDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `revokedGainDefersUntilGrantedAndDisposedActivityCannotChangeNewController` | 通过 |

<a id="audiofeatureanalyzertest"></a>
### AudioFeatureAnalyzerTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/audio/AudioFeatureAnalyzerTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/audio/AudioFeatureAnalyzerTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `shortOrSilenceAudioReturnsDefaults` | 通过 |
| 共享/桌面JVM | `syntheticBeatsAt120BpmAreAccuratelyDetected` | 通过 |

<a id="audiolyricalignmenttest"></a>
### AudioLyricAlignmentTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/AudioLyricAlignmentTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/AudioLyricAlignmentTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `alignsRepeatedChorusesToTheirActualAudioOccurrences` | 通过 |
| 共享/桌面JVM | `partialSpeechEventsAreIgnoredAndFinalWordTimesArePreserved` | 通过 |
| 共享/桌面JVM | `unrelatedLyricsNeverReceiveInventedTimestamps` | 通过 |
| 共享/桌面JVM | `zeroOnlyAndOutOfDurationLrcAreNotSynchronized` | 通过 |

<a id="audiosessionintegrationtest"></a>
### AudioSessionIntegrationTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AudioSessionIntegrationTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AudioSessionIntegrationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `bundledFfmpegMeasuresAndCachesWholeTrackLoudness` | 通过 |
| Android设备 | `commandsAreGrantedAndBackgroundTrackChangesUseOnlyGlobalVolume` | 通过 |
| Android设备 | `processorAmplifiesActualPcmAndSupportsMono` | 通过 |
| Android设备 | `r128JsonIsParsedOnTheAndroidRegexEngine` | 通过 |

<a id="benchmarktest"></a>
### BenchmarkTest

源码：[services/deploy/aliyun/test_benchmark.py](../../../services/deploy/aliyun/test_benchmark.py)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Python/deployment | `test_failed_requests_are_counted_and_not_reported_as_success` | 通过 |
| Python/deployment | `test_memory_guard_prevents_network_load` | 通过 |

<a id="bilibililoginservicetest"></a>
### BilibiliLoginServiceTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/media/BilibiliLoginServiceTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/media/BilibiliLoginServiceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `testCreateQrSessionFailure` | 通过 |
| 共享/桌面JVM | `testCreateQrSessionSuccess` | 通过 |
| 共享/桌面JVM | `testExtractAndMergeCookies` | 通过 |
| 共享/桌面JVM | `testPollQrStatusExpired` | 通过 |
| 共享/桌面JVM | `testPollQrStatusScanned` | 通过 |
| 共享/桌面JVM | `testPollQrStatusSuccessWithCookies` | 通过 |
| 共享/桌面JVM | `testPollQrStatusWaiting` | 通过 |

<a id="builtinmusicsourcestest"></a>
### BuiltinMusicSourcesTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/BuiltinMusicSourcesTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/BuiltinMusicSourcesTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `archiveListsIndividualPublicTracksAndSkipsRestrictedItems` | 通过 |
| 共享/桌面JVM | `qqResolvesFreeMediaAndLyricsButNeverRequestsPaidMedia` | 通过 |

<a id="catalogartworkrematchtest"></a>
### CatalogArtworkRematchTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/CatalogArtworkRematchTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/CatalogArtworkRematchTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `matchedLocalTrackKeepsCatalogArtworkWhenLocalArtworkIsMissing` | 通过 |

<a id="crossdevicesyncfixturetest"></a>
### CrossDeviceSyncFixtureTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/sync/CrossDeviceSyncFixtureTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/sync/CrossDeviceSyncFixtureTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `createsAndroidImportFixtureWithArtwork` | 通过 |

<a id="deepseekclienttest"></a>
### DeepSeekClientTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ai/DeepSeekClientTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ai/DeepSeekClientTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `cleansAndParsesAiGeneratedLrcContent` | 通过 |

<a id="desktopaudiooutputintegrationtest"></a>
### DesktopAudioOutputIntegrationTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/playback/DesktopAudioOutputIntegrationTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/DesktopAudioOutputIntegrationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `pauseSeekSpeedAndRapidReplacementReachRealAudioOutput` | 通过 |
| 共享/桌面JVM | `platformRecordsWithoutComposeAndDeduplicatesNearEndPauseResume` | 通过 |

<a id="desktopmanagedmp3convertertest"></a>
### DesktopManagedMp3ConverterTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/conversion/DesktopManagedMp3ConverterTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/conversion/DesktopManagedMp3ConverterTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `cancellingDecryptionPreservesSourceAndRemovesPartialOutput` | 通过 |
| 共享/桌面JVM | `convertsFlacInsideKgmaToManagedMp3AndCleansStaging` | 通过 |
| 共享/桌面JVM | `convertsWavToManaged320kMp3WithoutChangingSource` | 通过 |

<a id="desktopmusicimporttest"></a>
### DesktopMusicImportTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/platform/DesktopMusicImportTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/platform/DesktopMusicImportTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `badInputsDoNotPreventHealthyFilesFromImporting` | 通过 |
| 共享/桌面JVM | `cancellationDuringEnumerationKeepsSourceUntouched` | 通过 |
| 共享/桌面JVM | `fileManagerPayloadsHandleUnicodeSpacesAndUriEscapes` | 通过 |
| 共享/桌面JVM | `mixedFilesAndOverlappingFoldersImportOnceAndPreserveSources` | 通过 |
| 共享/桌面JVM | `oversizedUriPayloadIsRejected` | 通过 |
| 共享/桌面JVM | `unsupportedSelectionExplainsWhyNothingWasImported` | 通过 |

<a id="desktoppcmplayersessiontest"></a>
### DesktopPcmPlayerSessionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/audio/DesktopPcmPlayerSessionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/audio/DesktopPcmPlayerSessionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `bufferingEndsOnlyWhenPcmArrivesRatherThanWhenDeviceOpens` | 通过 |
| 共享/桌面JVM | `nativeCleanupDoesNotBlockStopOrTheNextPlayCommand` | 通过 |
| 共享/桌面JVM | `outputThatMakesNoProgressBacksOffAndReportsOneBoundedFailure` | 通过 |
| 共享/桌面JVM | `pausingDuringAPartialWriteStopsRetriesUntilResume` | 通过 |
| 共享/桌面JVM | `preparedPausedPlaybackWaitsForResumeAndIsNotAStalledDevice` | 通过 |
| 共享/桌面JVM | `replacementRejectsAnOldDecoderWhichFinishesAfterTheNewRequest` | 通过 |

<a id="desktopplaybackfailuretest"></a>
### DesktopPlaybackFailureTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/playback/DesktopPlaybackFailureTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/DesktopPlaybackFailureTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `actualDecoderFailuresCarryOriginAndRequestAcrossAtoBtoA` | 通过 |

<a id="desktopsyncpackageimporttest"></a>
### DesktopSyncPackageImportTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/platform/DesktopSyncPackageImportTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/platform/DesktopSyncPackageImportTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `cancellingBetweenPackagesRetainsCompletedPlaylistAndSource` | 通过 |
| 共享/桌面JVM | `conversionSelectionKeepsPackagedAudioFormatAndBytes` | 通过 |
| 共享/桌面JVM | `corruptedPackageDoesNotBlockHealthyPackageOrCommitBadAudio` | 通过 |
| 共享/桌面JVM | `emptyPlaylistPackageIsStillReportedAndSaved` | 通过 |
| 共享/桌面JVM | `folderAndUriDropFindUppercasePackageAndDeduplicateSelection` | 通过 |
| 共享/桌面JVM | `maliciousPackageCannotEscapeManagedDirectory` | 通过 |
| 共享/桌面JVM | `pickerIncludesPackagesAndAudioWithoutExposingUnrelatedFiles` | 通过 |
| 共享/桌面JVM | `selectingPackageRestoresMetadataAndPlaylistsAcrossRestart` | 通过 |

<a id="desktopwindowlifecycletest"></a>
### DesktopWindowLifecycleTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/DesktopWindowLifecycleTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/DesktopWindowLifecycleTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `linuxLyricsCanBeDraggedUnlockedAndStayInPlaceDuringUpdates` | 通过 |
| 共享/桌面JVM | `lyricsUpdateLockCloseAndReopenWhileMainWindowIsHidden` | 通过 |
| 共享/桌面JVM | `productionAppCanCloseLyricsWithoutShowingMainWindow` | 通过 |

<a id="ffmpegloudnessintegrationtest"></a>
### FfmpegLoudnessIntegrationTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/audio/FfmpegLoudnessIntegrationTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/audio/FfmpegLoudnessIntegrationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `wholeTrackAnalyzerDistinguishesWeakAndLoudAudio` | 通过 |

<a id="floatinglyricstest"></a>
### FloatingLyricsTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/FloatingLyricsTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/FloatingLyricsTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `nativePositionInPlayerStateOverridesMissingMetadataDuration` | 通过 |
| 共享/桌面JVM | `nativePositionKeepsBackgroundLyricsMovingWhenTrackDurationIsNotReady` | 通过 |
| 共享/桌面JVM | `translatedAndOriginalLinesShareTheOriginalTimeAxis` | 通过 |
| 共享/桌面JVM | `unavailableTranslationNeverMakesOriginalLyricsDisappear` | 通过 |

<a id="horizontalpageswipetest"></a>
### HorizontalPageSwipeTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/HorizontalPageSwipeTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/HorizontalPageSwipeTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `horizontalSwipesChangeOnePageAndDoNotWrapAtEdges` | 通过 |
| Android设备 | `verticalScrollAndSliderDragDoNotChangePage` | 通过 |

<a id="horizontalpageswipeunittest"></a>
### HorizontalPageSwipeUnitTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/HorizontalPageSwipeUnitTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/HorizontalPageSwipeUnitTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `appVersionAndHighlightsGeneratedFromReleaseNotes` | 通过 |
| 共享/桌面JVM | `swipePageTargetAdvancesToNextOnLeftSwipeAndPreviousOnRightSwipe` | 通过 |
| 共享/桌面JVM | `swipePageTargetRejectsAtBoundariesWithoutWrapping` | 通过 |
| 共享/桌面JVM | `swipePageTargetRejectsSubThresholdOrZeroThreshold` | 通过 |
| 共享/桌面JVM | `swipePageTargetSupportsFlingVelocityWhenDistanceIsSubThreshold` | 通过 |

<a id="httptest"></a>
### HttpTest

源码：[services/rendezvous/test_server.py](../../../services/rendezvous/test_server.py)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Python/rendezvous | `test_http_roundtrip_duplicate_keys_and_body_limits` | 通过 |

<a id="importreportmessagetest"></a>
### ImportReportMessageTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/ImportReportMessageTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/ImportReportMessageTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `emptyPlaylistPackageHasVisibleSuccessMessage` | 通过 |
| 共享/桌面JVM | `fullSuccessReportsConvertedCount` | 通过 |
| 共享/桌面JVM | `packageImportReportsPlaylistsAndDoesNotClaimAudioWasConverted` | 通过 |
| 共享/桌面JVM | `realConversionProblemIsReportedAsNotProcessed` | 通过 |

<a id="kggciphertest"></a>
### KggCipherTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/conversion/KggCipherTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/conversion/KggCipherTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `decryptStreamWorksCorrectly` | 通过 |
| 共享/桌面JVM | `detectsAudioFormatsCorrectly` | 通过 |
| 共享/桌面JVM | `parsesKggV5HeaderCorrectly` | 通过 |
| 共享/桌面JVM | `qmc2MapIsSelfInverting` | 通过 |
| 共享/桌面JVM | `qmc2Rc4IsSelfInvertingAcrossMultipleSegments` | 通过 |
| 共享/桌面JVM | `testKuGouKeyDatabaseLoadsWhenPresent` | 通过 |

<a id="kgmaciphertest"></a>
### KgmaCipherTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/conversion/KgmaCipherTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/conversion/KgmaCipherTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `audioSignaturesAreDetected` | 通过 |
| 共享/桌面JVM | `chunkedDecryptionMatchesSinglePass` | 通过 |
| 共享/桌面JVM | `decryptingEncryptedBytesRestoresOriginal` | 通过 |
| 共享/桌面JVM | `littleEndianHeaderFieldsAreDecoded` | 通过 |

<a id="kgmadevicefixturetest"></a>
### KgmaDeviceFixtureTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/conversion/KgmaDeviceFixtureTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/conversion/KgmaDeviceFixtureTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `writesSmallFixtureForAndroidConversionSmokeTest` | 通过 |

<a id="krcdecodertest"></a>
### KrcDecoderTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/KrcDecoderTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/KrcDecoderTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `decodesValidKrcData` | 通过 |

<a id="librarydetailtransitiontest"></a>
### LibraryDetailTransitionTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryDetailTransitionTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryDetailTransitionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `immediateBackInterruptsTheOpeningWithoutLeavingAStaleLayer` | 通过 |
| Android设备 | `openingCoversTheListAndClipsAtTheViewport` | 失败 |
| Android设备 | `reducedMotionDoesNotCrossfadeBothPagesTogether` | 通过 |
| Android设备 | `returningKeepsTheDetailAboveTheListInDarkMode` | 失败 |

<a id="librarynavigationtest"></a>
### LibraryNavigationTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `aboutShowsHighlightsForInstalledVersion` | 通过 |
| Android设备 | `assistantExampleFillsDraftWithoutSendingRequest` | 通过 |
| Android设备 | `assistantHeaderKeepsTitleOnOneLineAndActionsVisible` | 通过 |
| Android设备 | `assistantKeepsPlayerAndDockBoundsStable` | 通过 |
| Android设备 | `disabledRecommendationCardAppearsBeforeUserPlaylistsAndOpensSettings` | 失败 |
| Android设备 | `enabledRecommendationCardOpensItsOwnPlaylistPage` | 通过 |
| Android设备 | `enteringSongsDoesNotJumpToPlayingTrackAndTabScrollPositionIsRetained` | 通过 |
| Android设备 | `libraryBoundarySwipeOpensAssistant` | 通过 |
| Android设备 | `librarySwipesSelectTabsBeforeMainNavigationAndRetainListPosition` | 通过 |
| Android设备 | `playerCanExpandAndCloseRepeatedlyWithoutLeavingAnOverlay` | 通过 |
| Android设备 | `rapidPageChangesKeepTopEdgeCoveredInDarkMode` | 通过 |
| Android设备 | `rapidPageChangesKeepTopEdgeCoveredInLightMode` | 通过 |

<a id="librarypersistencetest"></a>
### LibraryPersistenceTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/platform/LibraryPersistenceTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/platform/LibraryPersistenceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `concurrentMergesAndFavoritesPreserveEveryTrack` | 通过 |
| 共享/桌面JVM | `corruptedPrimaryRecoversThePreviousValidRevision` | 通过 |
| 共享/桌面JVM | `relinkingMissingAudioPreservesIdentityFavoriteAndPlaylistAfterRestart` | 通过 |
| 共享/桌面JVM | `unreadableUiSettingsRecoverTheBackupBeforeTheNextWrite` | 通过 |

<a id="librarysearchindextest"></a>
### LibrarySearchIndexTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/LibrarySearchIndexTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/LibrarySearchIndexTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `indexedSearchPreservesChineseInitialsAndPunctuationMatching` | 通过 |
| 共享/桌面JVM | `searchesTenThousandTracksWithAReusableIndex` | 通过 |

<a id="linuxdesktopsupporttest"></a>
### LinuxDesktopSupportTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/platform/LinuxDesktopSupportTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/platform/LinuxDesktopSupportTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `dataPathsRespectXdgOverridesAndWindowsLayout` | 通过 |
| 共享/桌面JVM | `existingLinuxLibraryIsKeptUntilXdgDirectoryExists` | 通过 |
| 共享/桌面JVM | `ffmpegUsesHostNameAndPackagedOrExplicitExecutable` | 通过 |
| 共享/桌面JVM | `linuxCredentialsRejectSymlinkedKeyAndDirectory` | 通过 |
| 共享/桌面JVM | `linuxCredentialsRejectTamperingWrongDirectoryAndMissingKey` | 通过 |
| 共享/桌面JVM | `linuxCredentialsSurviveRestartAndUsePrivatePermissions` | 通过 |
| 共享/桌面JVM | `linuxNetworkCostOnlyAcceptsConnectedUnmeteredInterfaces` | 通过 |

<a id="listentogethertest"></a>
### ListenTogetherTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/ListenTogetherTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/ListenTogetherTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `authenticatedAudioPauseSeekAndNextTrackStayWithinTemporaryCache` | 通过 |
| 共享/桌面JVM | `invitationRoundTripsIpv6AndRejectsAmbiguousCapabilities` | 通过 |
| 共享/桌面JVM | `invitationSupportsMultipleCandidatesAndRoundTrips` | 通过 |
| 共享/桌面JVM | `ipv6LoopbackCanCarryTheEncryptedSession` | 通过 |
| 共享/桌面JVM | `leavingAndImmediatelyRejoiningDoesNotDeleteNewSessionMedia` | 通过 |
| 共享/桌面JVM | `malformedAuthenticatedFramesNeverBecomePlayableFiles` | 通过 |
| 共享/桌面JVM | `protocolRejectsDeepJsonDuplicateKeysAndMalformedUtf8` | 通过 |
| 共享/桌面JVM | `racedConnectionSuccessfullySelectsReachableCandidate` | 通过 |
| 共享/桌面JVM | `roomCapacityRejectionAndEndedInvitationCannotJoinANewRoom` | 通过 |
| 共享/桌面JVM | `wrongPinAndWrongKeyCannotReadAudioOrOccupyTheRoom` | 通过 |

<a id="listeningsessiondevicetest"></a>
### ListeningSessionDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/ListeningSessionDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/ListeningSessionDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `activityRecreationBackgroundPauseSeekAndNativeRepeatKeepAccurateStats` | 通过 |

<a id="listeningsessiontrackertest"></a>
### ListeningSessionTrackerTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/ListeningSessionTrackerTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/ListeningSessionTrackerTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `bufferingSeekSpeedAndRemoteAudioDoNotInflateDuration` | 通过 |
| 共享/桌面JVM | `jitterBetweenWallClockAndAudioClockDoesNotLoseTimeOnEveryTick` | 通过 |
| 共享/桌面JVM | `pauseNearEndDoesNotCompleteAgainAndRepeatStartsAnotherPass` | 通过 |
| 共享/桌面JVM | `quarterSecondNativeUpdatesAccumulateAcrossPauseWithoutUiTimer` | 通过 |

<a id="listeningstatsrepositorytest"></a>
### ListeningStatsRepositoryTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/playback/ListeningStatsRepositoryTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/ListeningStatsRepositoryTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `serviceAndRecreatedUiShareOneStoreAndCannotOverwriteEachOther` | 通过 |
| 共享/桌面JVM | `uiSnapshotDoesNotWaitForTheDiskStoresMonitor` | 通过 |
| 共享/桌面JVM | `writeFailureIsVisibleAndAcceptedIncrementIsNotDuplicatedOnFlushRetry` | 通过 |

<a id="listeningstatsstoretest"></a>
### ListeningStatsStoreTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/playback/ListeningStatsStoreTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/ListeningStatsStoreTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `audioEffectPresetFromIdWorks` | 通过 |
| 共享/桌面JVM | `batchedWritesFlushWithoutLosingAcceptedSecondsAndReportFailures` | 通过 |
| 共享/桌面JVM | `corruptHistoryIsPreservedAndValidBackupRecovers` | 通过 |
| 共享/桌面JVM | `formatsListeningSecondsNicely` | 通过 |
| 共享/桌面JVM | `recordsPlaybackSecondsAndSavesAtomically` | 通过 |

<a id="localscreenshotocrtest"></a>
### LocalScreenshotOcrTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LocalScreenshotOcrTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LocalScreenshotOcrTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `bundledOfflineOcrReadsBilingualPlaylistAndReleasesSessions` | 通过 |
| Android设备 | `wideScreenshotsUseRealDecoderScaleWithinSliceBounds` | 通过 |

<a id="loudnessanalysistest"></a>
### LoudnessAnalysisTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/audio/LoudnessAnalysisTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/audio/LoudnessAnalysisTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `loudTrackIsAttenuatedAndVeryWeakGainIsBounded` | 通过 |
| 共享/桌面JVM | `silenceAndIncompleteOutputAreNotCached` | 通过 |
| 共享/桌面JVM | `weakTrackUsesTruePeakHeadroom` | 通过 |
| 共享/桌面JVM | `wholeTrackAnalysisDoesNotSeekOrLimitDuration` | 通过 |

<a id="lrcliblyricsrepositorytest"></a>
### LrclibLyricsRepositoryTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/LrclibLyricsRepositoryTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/LrclibLyricsRepositoryTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `exactPlainLyricsContinueSearchingForSynchronizedVersion` | 通过 |
| 共享/桌面JVM | `fetchesExactLyricsAndUsesPersistentCache` | 通过 |
| 共享/桌面JVM | `parsesMultipleTimestampsAndFractions` | 通过 |

<a id="lyricscrolltest"></a>
### LyricScrollTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/LyricScrollTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/LyricScrollTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `measuredBilingualAndWrappedRowsShareTheSameCenter` | 通过 |
| 共享/桌面JVM | `oversizedLinesKeepTheirBeginningReadable` | 通过 |
| 共享/桌面JVM | `usesTheViewportCoordinateSystemIncludingTopPadding` | 通过 |

<a id="lyricsacquisitiontest"></a>
### LyricsAcquisitionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/LyricsAcquisitionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/lyrics/LyricsAcquisitionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `cachedLrclibMissDoesNotBlockSourceFallbackAndSuccessPersists` | 通过 |
| 共享/桌面JVM | `cachedPlainLyricsCanRecoverFromLrclibWithoutInstalledSources` | 通过 |
| 共享/桌面JVM | `failedOrPlainSourcePreservesCachedTextAndOffsetAndCancellationPropagates` | 通过 |
| 共享/桌面JVM | `freshAndCachedPlainLyricsContinueToSynchronizedSource` | 通过 |
| 共享/桌面JVM | `localPlainLyricsDoNotBlockSynchronizedSourceOrLoseTextOnFailure` | 通过 |
| 共享/桌面JVM | `modelTextNeverCreatesASynchronizedTimeline` | 通过 |
| 共享/桌面JVM | `modelUnavailableIsNotReportedAsANetworkOrSearchFailure` | 通过 |
| 共享/桌面JVM | `publicSourceFindsUserReportedRecordingWhenExplicitlyEnabled` | 条件跳过 |
| 共享/桌面JVM | `publicSynchronizedLyricsReplaceReportedRecordingsPlainCacheWhenExplicitlyEnabled` | 条件跳过 |
| 共享/桌面JVM | `sourceKeepsUsablePlainLyricsWhenLaterMatchingCandidateFails` | 通过 |
| 共享/桌面JVM | `sourceLyricsMatchRecordingAndNeverFetchAudio` | 通过 |
| 共享/桌面JVM | `sourcesPreferSynchronizedLyricsAcrossSourcesAndWithinMatchingCandidates` | 通过 |
| 共享/桌面JVM | `synchronizedSearchCandidateWinsOverHigherScoringPlainOrInvalidTimeline` | 通过 |

<a id="lyricsmotiontest"></a>
### LyricsMotionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsMotionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsMotionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `focusBandAndSelectedLineMoveTogetherInBothThemes` | 通过 |
| 共享/桌面JVM | `previewReusesTheSameLineWhileItSlidesIntoFocus` | 通过 |
| 共享/桌面JVM | `reducedMotionSeeksImmediatelyAndTranslationArrivalPreservesListIdentity` | 通过 |
| 共享/桌面JVM | `wrappedLineMovesThroughIntermediatePositionsWithoutReflow` | 通过 |

<a id="lyricsplayerregressiondevicetest"></a>
### LyricsPlayerRegressionDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsPlayerRegressionDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsPlayerRegressionDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `coverAndFullLyricsFollowNativePositionWithVisibleHighlight` | 通过 |
| Android设备 | `largeTextKeepsCoverPreviewAndTransportReachable` | 通过 |

<a id="lyricsplayerregressiontest"></a>
### LyricsPlayerRegressionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsPlayerRegressionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsPlayerRegressionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `coverPreviewAndImmersiveFocusFollowNativePositionAtAllWindowSizes` | 通过 |

<a id="lyricspreviewmotiondevicetest"></a>
### LyricsPreviewMotionDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsPreviewMotionDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsPreviewMotionDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `onePersistentRowSlidesIntoFocusAcrossSeveralFrames` | 通过 |
| Android设备 | `reducedMotionAndBackwardSeekKeepTheSameFocus` | 通过 |

<a id="lyricsscrolldevicetest"></a>
### LyricsScrollDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsScrollDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsScrollDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `browsingWaitsThenResumesWithoutANewLyricTimestamp` | 通过 |
| Android设备 | `changingTracksWithTheSameLinesAndTimestampStillCentersTheNewList` | 通过 |
| Android设备 | `firstLastResizeAndLargeTextStayCenteredWithReducedMotion` | 通过 |
| Android设备 | `initialAndWrappedBilingualLinesAreCentered` | 通过 |
| Android设备 | `nextLinePassesThroughIntermediatePositionsWithoutChangingRowHeight` | 通过 |
| Android设备 | `plainTextBrowsingDoesNotSeekOrShowFollowControls` | 通过 |
| Android设备 | `rapidlyReversingTheTargetDoesNotReplayOrSnap` | 通过 |
| Android设备 | `returnButtonImmediatelyRecentersEvenWhilePaused` | 通过 |
| Android设备 | `seekingToAnOffscreenLineStartsByScrollingTheExistingRows` | 通过 |
| Android设备 | `tappingUsesOffsetAndClampsSeekToTheAudioRange` | 通过 |

<a id="lyricssessiontest"></a>
### LyricsSessionTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/LyricsSessionTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/LyricsSessionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `acceptedOffsetEditInvalidatesAnInFlightLookup` | 通过 |
| 共享/桌面JVM | `cancelledLookupCannotPublishAfterSelectingTheSameTrackAgain` | 通过 |
| 共享/桌面JVM | `manualRefreshWinsOverAnOlderAutomaticLookup` | 通过 |

<a id="lyricstranslationcontrollertest"></a>
### LyricsTranslationControllerTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsTranslationControllerTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsTranslationControllerTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `blankLanguageAndUnavailableServiceReportAnActionableFailure` | 通过 |
| 共享/桌面JVM | `failedRetranslationKeepsPreviousTranslationAndCanBeRetried` | 通过 |
| 共享/桌面JVM | `languageChangeAndReplacedSourceRejectOldResult` | 通过 |
| 共享/桌面JVM | `slowCacheMissCannotEraseANewManualTranslation` | 通过 |
| 共享/桌面JVM | `trackChangeRetiresOldRequestAndAllowsImmediateTranslationOfNewSong` | 通过 |
| 共享/桌面JVM | `trimmedLanguageAndOriginalOnlyModeShowTranslationWithoutChangingTimeline` | 通过 |

<a id="lyricstranslationdevicetest"></a>
### LyricsTranslationDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsTranslationDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsTranslationDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `clickingTranslateShowsBilingualLyricsFromOriginalOnlyMode` | 通过 |
| Android设备 | `failedRequestShowsErrorBesideControlsAndAllowsRetry` | 通过 |

<a id="metadataenrichertest"></a>
### MetadataEnricherTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/enrich/MetadataEnricherTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/enrich/MetadataEnricherTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `cleansTrackTitleProperly` | 通过 |
| 共享/桌面JVM | `detectMissingMetadataHelpers` | 通过 |
| 共享/桌面JVM | `publicCatalogMatchingRejectsWrongSongsAndAllowsUnknownArtist` | 通过 |
| 共享/桌面JVM | `skipsAlreadyCompleteTrackWhenNotForceRefresh` | 通过 |

<a id="musicaiwiretest"></a>
### MusicAiWireTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAiWireTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAiWireTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `brokenConnectionIsNotReplayedAndReportsUnknownUsage` | 通过 |
| 共享/桌面JVM | `officialFlashRequestsFinalContentWhileGenericEndpointsAndReasonersKeepTheirContract` | 通过 |

<a id="musicassistanttest"></a>
### MusicAssistantTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicAssistantTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `automaticRecommendationWaitsForNetworkButManualRequestCanProceed` | 通过 |
| 共享/桌面JVM | `batchRetryNeverRunsPaidLyricsAndUndoClearsOldRejection` | 通过 |
| 共享/桌面JVM | `builtInHelpDoesNotSendAnyModelRequest` | 通过 |
| 共享/桌面JVM | `cancellationAfterAudioImportKeepsItInPendingRecommendationCleanup` | 通过 |
| 共享/桌面JVM | `chatToolExecutionExceeds16StepsWithoutLimit` | 通过 |
| 共享/桌面JVM | `chatWithMultipleSessionsMaintainsIsolationAndAttribution` | 通过 |
| 共享/桌面JVM | `cleanupProtectsSavedAndPlayingAudio` | 通过 |
| 共享/桌面JVM | `cleanupReleasesOnlyOwnedTemporaryAudioAndCanRestoreIt` | 通过 |
| 共享/桌面JVM | `dislikeDeletesEvenFavoriteAndEveryPlaylistMembership` | 通过 |
| 共享/桌面JVM | `emptyOrNonJsonTranslationReturnsUsefulErrorWithoutCaching` | 通过 |
| 共享/桌面JVM | `failedAutomaticRecommendationDoesNotPayAgainEachMinuteOrAfterRestart` | 通过 |
| 共享/桌面JVM | `failedRepeatedToolsStopLocallyWithoutCallingTheModelForever` | 通过 |
| 共享/桌面JVM | `feedbackSurvivesRolloverCleanupAndIsPassedToNextAiRecommendation` | 通过 |
| 共享/桌面JVM | `generatedLyricsUseAudioTimestampsAndCacheWithoutChargingTwice` | 通过 |
| 共享/桌面JVM | `interruptedTaskIsRecoveredAndSpeechBudgetRejectsBeforeUpload` | 通过 |
| 共享/桌面JVM | `laterSearchKeepsPreviouslyReturnedSourceIdsAvailableForDownload` | 通过 |
| 共享/桌面JVM | `libraryToolReturnsPlayableLinksWithoutExposingFileLocations` | 通过 |
| 共享/桌面JVM | `localAudioOutsideManagedRootCannotBecomeCleanupTarget` | 通过 |
| 共享/桌面JVM | `localRecognitionFailureNeverFallsBackToPaidApi` | 通过 |
| 共享/桌面JVM | `localScreenshotImportNeedsNoApiKeyAndCompletesTitlesBeforeDownload` | 通过 |
| 共享/桌面JVM | `negatedAndPlaylistOnlyRequestsCannotDeleteAudio` | 通过 |
| 共享/桌面JVM | `networkSettingRoundTripsAndLegacySettingsKeepPreviousBehavior` | 通过 |
| 共享/桌面JVM | `oldAutomaticAiSettingsEnterCooldownWithoutCallingTheModelOnUpgrade` | 通过 |
| 共享/桌面JVM | `originalCandidatesBeatLiveCoverAndUnrelatedArtist` | 通过 |
| 共享/桌面JVM | `playingOldRecommendationIsDeferredInsteadOfDeletingAnOpenFile` | 通过 |
| 共享/桌面JVM | `playlistManagementToolsNeverDeleteAudioAndSnapshotPrefixStaysStable` | 通过 |
| 共享/桌面JVM | `recentlyRemovedRecommendationIsNotDownloadedAgain` | 通过 |
| 共享/桌面JVM | `recommendationRetainedWhenUnlistenedEvenIfIntervalExpired` | 通过 |
| 共享/桌面JVM | `recommendationRolloverDeletesOnlyUnsavedDownloads` | 通过 |
| 共享/桌面JVM | `recommendationVotesAndCommentsDoNotFavoriteOrDeleteAudioOrCallModel` | 通过 |
| 共享/桌面JVM | `retryCoverDoesNotDownloadAudioAgainAndHistorySurvivesRestart` | 通过 |
| 共享/桌面JVM | `saveRecommendationToNewPlaylistSurvivesRolloverAndReload` | 通过 |
| 共享/桌面JVM | `screenshotRecognitionIsCachedAndExistingSongsAreNotDownloadedAgain` | 通过 |
| 共享/桌面JVM | `screenshotSelectionCanBeEditedAndDiscardedWithoutDownloading` | 通过 |
| 共享/桌面JVM | `sourceDownloadFailureRemainsVisibleWhenBilibiliFallbackAlsoFails` | 通过 |
| 共享/桌面JVM | `sourcePreviewUsesBoundedTemporaryFileWithoutImportingOrCallingAi` | 通过 |
| 共享/桌面JVM | `tokenUsageRestrictionsAreCancelledAndDoNotRejectRequests` | 通过 |
| 共享/桌面JVM | `translationCacheNormalizesTargetLanguageLikeFreshRequests` | 通过 |
| 共享/桌面JVM | `translationCachesPerTextAndTargetAndPreservesTimestamps` | 通过 |
| 共享/桌面JVM | `unavailableNewBatchKeepsPreviousAudioAndTimestamp` | 通过 |
| 共享/桌面JVM | `unsolicitedDeleteToolIsRejectedButExplicitDeleteRuns` | 通过 |
| 共享/桌面JVM | `wrongTranslationLineCountDoesNotCacheOrModifyOriginal` | 通过 |

<a id="musicfeaturesintegrationtest"></a>
### MusicFeaturesIntegrationTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicFeaturesIntegrationTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicFeaturesIntegrationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `importedAudioHasEditableTagsAndCoverAndDislikeCanPhysicallyDeleteIt` | 通过 |
| Android设备 | `longScreenshotsAreSplitAndCredentialsAreEncrypted` | 通过 |

<a id="musicjsonstoretest"></a>
### MusicJsonStoreTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicJsonStoreTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicJsonStoreTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `corruptPrimaryRecoversWithoutOverwritingGoodBackupOnNextWrite` | 通过 |
| 共享/桌面JVM | `twoCorruptCopiesStopInsteadOfSilentlyResettingSettingsAndHistory` | 通过 |

<a id="musicsourceregistrytest"></a>
### MusicSourceRegistryTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicSourceRegistryTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicSourceRegistryTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `builtinSourcesInstalledSuccessfullyAndExposeCoreCapabilities` | 通过 |
| 共享/桌面JVM | `builtinUpgradeAddsSourcesOnceWithoutRestoringRemovedOrEnablingDisabledSources` | 通过 |
| 共享/桌面JVM | `cancellationInterruptsInfiniteScriptAndRuntimeCanBeUsedAgain` | 通过 |
| 共享/桌面JVM | `invalidReplacementAndUnsupportedDependencyKeepInstalledSource` | 通过 |
| 共享/桌面JVM | `publicTransportRejectsLocalAddressesAndCredentialUrls` | 通过 |
| 共享/桌面JVM | `realQuickJsRunsAsyncMusicFreeCoreAndPersistsInstallation` | 通过 |
| 共享/桌面JVM | `recommendationsWithEmptyArtistsUsesDefaultPopularSeeds` | 通过 |
| 共享/桌面JVM | `runtimeSupportsCryptoJsWithoutExposingHostClasses` | 通过 |
| 共享/桌面JVM | `searchLimitLeavesRoomForAlternativeSources` | 通过 |

<a id="musicstagedevicetest"></a>
### MusicStageDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `largeTextKeepsTransportAndPlaybackSettingsReachable` | 通过 |
| Android设备 | `lyricsQueueSeekAndDismissUseRealControls` | 失败 |
| Android设备 | `playingAtmosphereAndNextTrackRemainInteractive` | 通过 |

<a id="musicstageframemetricstest"></a>
### MusicStageFrameMetricsTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageFrameMetricsTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/MusicStageFrameMetricsTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `playingStageReportsActualWindowFrameDurations` | 失败 |

<a id="musictaskgroupstest"></a>
### MusicTaskGroupsTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/MusicTaskGroupsTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/MusicTaskGroupsTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `groupsNestedStagesWithoutCombiningSeparateImports` | 通过 |
| 共享/桌面JVM | `orphanAndCyclicHistoricalParentsDoNotLoopOrLoseEntries` | 通过 |
| 共享/桌面JVM | `successfulSongHidesRecoveredSourceAttemptButKeepsFailedCoverRetry` | 通过 |

<a id="pcmgainprocessortest"></a>
### PcmGainProcessorTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/audio/PcmGainProcessorTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/audio/PcmGainProcessorTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `allAudioEffectPresetsProcessPcmSafely` | 通过 |
| 共享/桌面JVM | `largeGainNeverWrapsOrClipsAbruptly` | 通过 |
| 共享/桌面JVM | `masterMuteProducesSilence` | 通过 |
| 共享/桌面JVM | `newGainRampsInsteadOfJumping` | 通过 |
| 共享/桌面JVM | `playbackTempoRemainsWithinFfmpegFilterRange` | 通过 |
| 共享/桌面JVM | `positiveDigitalPreampActuallyAmplifiesPcm` | 通过 |
| 共享/桌面JVM | `surroundPresetWidensStereoSpread` | 通过 |
| 共享/桌面JVM | `vocalSuppressionSuppressesCenterChannelWhileRetainingStereo` | 通过 |

<a id="persistencequeuetest"></a>
### PersistenceQueueTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/PersistenceQueueTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/PersistenceQueueTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `coalescedWritesDoNotJumpAcrossOrderedActions` | 通过 |
| 共享/桌面JVM | `diskWritesStayOrderedAndFailureDoesNotDropFollowingActions` | 通过 |
| 共享/桌面JVM | `disposingTheUiFinishesAcceptedWrites` | 通过 |
| 共享/桌面JVM | `rapidSliderChangesKeepLatestValuesForEachTrackAndGlobalVolume` | 通过 |

<a id="pinyinutilstest"></a>
### PinyinUtilsTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/util/PinyinUtilsTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/util/PinyinUtilsTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `testGetCharInitial` | 通过 |
| 共享/桌面JVM | `testMatches` | 通过 |
| 共享/桌面JVM | `testToPinyinInitials` | 通过 |

<a id="plainsyncpackagetest"></a>
### PlainSyncPackageTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/sync/PlainSyncPackageTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/sync/PlainSyncPackageTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `exportAndImportSucceedWhenArtworkIsMissing` | 通过 |
| 共享/桌面JVM | `rejectsCorruptedAudio` | 通过 |
| 共享/桌面JVM | `rejectsLegacyEncryptedPackage` | 通过 |
| 共享/桌面JVM | `roundTripPreservesMetadataAudioAndArtwork` | 通过 |

<a id="playbackissuetest"></a>
### PlaybackIssueTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/PlaybackIssueTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/PlaybackIssueTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `oldSameTrackAttemptIsRejectedAfterAtoBtoA` | 通过 |
| 共享/桌面JVM | `previewAndStorageFailureNeverInterruptMainPlayback` | 通过 |

<a id="playbackmodepersistencetest"></a>
### PlaybackModePersistenceTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/PlaybackModePersistenceTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/PlaybackModePersistenceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `defaultPlaybackModePreferenceIsDisabledAndRepeatOff` | 通过 |
| 共享/桌面JVM | `desktopPlatformServicesPersistsAndLoadsPlaybackMode` | 通过 |
| 共享/桌面JVM | `playerStatePreservesPlaybackModeWhenResettingCurrentTrack` | 通过 |

<a id="playbackmodewritertest"></a>
### PlaybackModeWriterTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/PlaybackModeWriterTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/PlaybackModeWriterTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `blockedOldWriteCannotOverwriteLastActionAndCloseDrains` | 通过 |

<a id="playbackqueueandfiltertest"></a>
### PlaybackQueueAndFilterTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackQueueAndFilterTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackQueueAndFilterTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `testBuildPlayNextQueue_ignoresUnplayableOrCurrentTrack` | 通过 |
| 共享/桌面JVM | `testBuildPlayNextQueue_insertsAfterCurrentTrack` | 通过 |
| 共享/桌面JVM | `testBuildPlayNextQueue_insertsAtBeginningWhenCurrentTrackNull` | 通过 |
| 共享/桌面JVM | `testBuildPlayNextQueue_movesExistingTrackWithoutDuplicate` | 通过 |
| 共享/桌面JVM | `testBuildRemoveFromQueue` | 通过 |
| 共享/桌面JVM | `testBuildReorderedQueue` | 通过 |
| 共享/桌面JVM | `testFilterAndSortTracks_filterQuery` | 通过 |
| 共享/桌面JVM | `testFilterAndSortTracks_losslessAndQuality` | 通过 |
| 共享/桌面JVM | `testFilterAndSortTracks_missingCoverAndMetadata` | 通过 |
| 共享/桌面JVM | `testFilterAndSortTracks_sortDuration` | 通过 |

<a id="playbackqueueresolutiontest"></a>
### PlaybackQueueResolutionTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/PlaybackQueueResolutionTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/PlaybackQueueResolutionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `fallsBackToFullLibraryWhenAllCustomTracksAreUnplayable` | 通过 |
| 共享/桌面JVM | `fallsBackToFullLibraryWhenCustomQueueIsNull` | 通过 |
| 共享/桌面JVM | `filtersOutUnplayableTracksInCustomQueue` | 通过 |
| 共享/桌面JVM | `nativeQueueUsesTheSelectedAudioInsteadOfAnOlderCopyWithTheSameId` | 通过 |
| 共享/桌面JVM | `recommendationQueueKeepsItsOrderAndSkipsEntriesWithoutAudio` | 通过 |
| 共享/桌面JVM | `scopesPlaybackToSpecificPlaylist` | 通过 |
| 共享/桌面JVM | `selectedDownloadIsIncludedEvenBeforeLibraryReloadReachesTheUi` | 通过 |

<a id="playbacksessiontest"></a>
### PlaybackSessionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/PlaybackSessionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/PlaybackSessionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `clearingAnEmptyQueueCancelsAFileCheckThatFinishesLate` | 通过 |
| 共享/桌面JVM | `consecutiveQueueCommandsReadTheCommittedInMemoryState` | 通过 |
| 共享/桌面JVM | `rapidRequestsRejectAnObsoleteNonCancellableFileCheck` | 通过 |
| 共享/桌面JVM | `removingCurrentTrackCannotRestoreItThroughTheOldCapturedQueue` | 通过 |

<a id="playbacksettingsdialogdevicetest"></a>
### PlaybackSettingsDialogDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PlaybackSettingsDialogDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PlaybackSettingsDialogDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `volumeOpensAdjustsReopensAndLeavesPlaybackResponsive` | 通过 |

<a id="playbacksettingsdialogtest"></a>
### PlaybackSettingsDialogTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlaybackSettingsDialogTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlaybackSettingsDialogTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `dialogIsolatesEveryContentSlotFromHostGlassWithoutChangingHostState` | 通过 |
| 共享/桌面JVM | `volumeOpensAdjustsAndClosesWithPlaybackControlsStillResponsive` | 通过 |

<a id="playbacksleeptimertest"></a>
### PlaybackSleepTimerTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackSleepTimerTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackSleepTimerTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `countdownUsesDeadlineAndExpiresOnlyOnce` | 通过 |
| 共享/桌面JVM | `endOfTrackStopsOnceWithoutStartingAnotherQueueItem` | 通过 |
| 共享/桌面JVM | `replacingAndCancellingTimersNeverPausesFromOldDeadline` | 通过 |

<a id="playbacksnapshottest"></a>
### PlaybackSnapshotTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackSnapshotTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackSnapshotTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `malformedSnapshotDoesNotPreventStartup` | 通过 |
| 共享/桌面JVM | `persistsCurrentTrackQueuePositionAndPlaylist` | 通过 |

<a id="playbackstatestoretest"></a>
### PlaybackStateStoreTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackStateStoreTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PlaybackStateStoreTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `audioRefreshPreservesProtectionFromPreviousNativeStatus` | 通过 |
| 共享/桌面JVM | `bufferingTransitionsReflectInPlayerStateAndClearOnPlayback` | 通过 |
| 共享/桌面JVM | `clearingTemporaryPlaybackCancelsAStartThatFinishesAfterLeavingTheRoom` | 通过 |
| 共享/桌面JVM | `disposedStateRejectsLateAudioResults` | 通过 |
| 共享/桌面JVM | `failureExitsPreparingAndBufferingStates` | 通过 |
| 共享/桌面JVM | `lateAnalysisCannotOverwriteNewSong` | 通过 |
| 共享/桌面JVM | `manualVolumeAndGainWinOverPendingReadsAndAnalysis` | 通过 |
| 共享/桌面JVM | `oldNativeTrackEventsCannotCancelTheLatestPlaybackRequest` | 通过 |
| 共享/桌面JVM | `pauseWhilePreparingPreventsPlaybackStartOnReady` | 通过 |
| 共享/桌面JVM | `previousNativePauseCannotCancelASingleTrackSelection` | 通过 |
| 共享/桌面JVM | `previousNativePlayingCannotUndoAnExplicitPauseDuringPreparation` | 通过 |
| 共享/桌面JVM | `rapidSwitchIgnoresLegacyCalibrationAndAppliesOnlyLatestBalancedGain` | 通过 |
| 共享/桌面JVM | `refreshingAudioWhileLoadingKeepsThePendingPlaybackRequest` | 通过 |
| 共享/桌面JVM | `switchingDuringQueuePreparationCannotStartThePreviousTrack` | 通过 |
| 共享/桌面JVM | `volumeAndCachedGainAreAppliedBeforePlaybackStarts` | 通过 |

<a id="playerdialogsdevicetest"></a>
### PlayerDialogsDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PlayerDialogsDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PlayerDialogsDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `audioEffectsOpenSelectAndCloseWithoutStoppingPlayerControls` | 通过 |

<a id="playerexpansiontest"></a>
### PlayerExpansionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlayerExpansionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlayerExpansionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `coverConnectsMeasuredEndpointsAndRapidReversalKeepsItsPosition` | 通过 |
| 共享/桌面JVM | `realShellConnectsItsMiniPlayerAtEveryWindowClass` | 通过 |
| 共享/桌面JVM | `reducedMotionAndKeyboardKeepTheCoverStationaryAndVisible` | 通过 |
| 共享/桌面JVM | `upwardReversalAndLyricsScrollingKeepThePlayerOpen` | 通过 |
| 共享/桌面JVM | `verticalDragTracksThePointerShortPullReturnsAndLongPullDismisses` | 通过 |

<a id="playerrecoveryuitest"></a>
### PlayerRecoveryUiTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlayerRecoveryUiTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlayerRecoveryUiTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `audioDialogHasBoundedListAndSelectionDoesNotInvokePlaybackWorkUntilChosen` | 通过 |
| 共享/桌面JVM | `emptyLyricsHaveOneSearchActionAndReachableToolsWithLargeText` | 通过 |
| 共享/桌面JVM | `favoriteCardOnlyShowsSongCount` | 通过 |

<a id="playlistarchiveformattest"></a>
### PlaylistArchiveFormatTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/PlaylistArchiveFormatTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/PlaylistArchiveFormatTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `flacAudioKeepsEncodingExtensionMimeAndArtwork` | 通过 |

<a id="promoaisetupdevicetest"></a>
### PromoAiSetupDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoAiSetupDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoAiSetupDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `configurePrivateCameraThroughEphemeralSocket` | 条件跳过 |

<a id="promoassistantprobetest"></a>
### PromoAssistantProbeTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/PromoAssistantProbeTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/PromoAssistantProbeTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `captureRealAssistantAnswerWhenConfigured` | 条件跳过 |

<a id="promocapturedevicetest"></a>
### PromoCaptureDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoCaptureDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoCaptureDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `recordNativePlayerStory` | 条件跳过 |

<a id="promofeaturescapturedevicetest"></a>
### PromoFeaturesCaptureDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoFeaturesCaptureDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PromoFeaturesCaptureDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `recordAssistantEntryAndRealEncryptedRoom` | 条件跳过 |

<a id="publicsourcednstest"></a>
### PublicSourceDnsTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/PublicSourceDnsTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/PublicSourceDnsTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `fakeIpUsesVerifiedPublicDestinationsAndCachesOnlySuccessfulResults` | 通过 |
| 共享/桌面JVM | `fallbackResponsesStillRejectPrivateFakeOrMixedAddresses` | 通过 |
| 共享/桌面JVM | `ordinaryPublicDnsDoesNotContactAnotherResolver` | 通过 |
| 共享/桌面JVM | `privateAndLiteralTargetsCannotUseVpnFallback` | 通过 |

<a id="recommendationfeedbackpolicytest"></a>
### RecommendationFeedbackPolicyTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/RecommendationFeedbackPolicyTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/RecommendationFeedbackPolicyTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `favoritesHaveMoreSeedWeightThanLikesAndAiSummaryIsBounded` | 通过 |
| 共享/桌面JVM | `feedbackUsesMusicalIdentityAndRemovingVoteKeepsComment` | 通过 |
| 共享/桌面JVM | `likesAndNegativeCommentsChangeCandidateRankingWithoutBanningAnArtist` | 通过 |

<a id="recommendationplaybacktest"></a>
### RecommendationPlaybackTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/RecommendationPlaybackTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/RecommendationPlaybackTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `failedRestoreKeepsPlaybackAndAllowsRetry` | 通过 |
| 共享/桌面JVM | `lateRestoreCannotOverrideANewerRecommendationClick` | 通过 |
| 共享/桌面JVM | `recommendationPlaybackUsesBatchOrderAndExcludesUnrelatedLibraryTracks` | 通过 |
| 共享/桌面JVM | `restoredAudioIsPlayableBeforeTheUiLibrarySnapshotUpdates` | 通过 |

<a id="recommendationpolicytest"></a>
### RecommendationPolicyTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/RecommendationPolicyTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/RecommendationPolicyTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `cooldownSurvivesFileRemovalAndUsesRollingDays` | 通过 |
| 共享/桌面JVM | `duplicateFilesCountOnceAndVersionsRemainSeparate` | 通过 |
| 共享/桌面JVM | `legacySettingsDefaultToSourceModeAndRoundTripNewControls` | 通过 |
| 共享/桌面JVM | `reasonsOnlyUseRealSavedEvidence` | 通过 |

<a id="registrytest"></a>
### RegistryTest

源码：[services/rendezvous/test_server.py](../../../services/rendezvous/test_server.py)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Python/rendezvous | `test_capacity_and_rate_limits_bound_memory` | 通过 |
| Python/rendezvous | `test_concurrent_challenges_do_not_invalidate_each_other` | 通过 |
| Python/rendezvous | `test_expiration_removal_and_withdraw_are_not_online_status_oracles` | 通过 |
| Python/rendezvous | `test_only_mutually_authorized_online_partners_can_lookup` | 通过 |
| Python/rendezvous | `test_private_content_unknown_fields_and_unbounded_addresses_are_rejected` | 通过 |
| Python/rendezvous | `test_replay_tamper_expired_challenge_and_cross_action_signature_fail` | 通过 |

<a id="relaytest"></a>
### RelayTest

源码：[services/relay/test_websocket_server.py](../../../services/relay/test_websocket_server.py)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Python/relay | `test_capacity_rejects_without_success` | 通过 |
| Python/relay | `test_corrupt_budget_is_preserved_and_refuses_startup` | 通过 |
| Python/relay | `test_default_does_not_trust_client_ip_headers` | 通过 |
| Python/relay | `test_durable_budget_cannot_reset_on_restart` | 通过 |
| Python/relay | `test_invalid_hello_and_wait_timeout_release_slots` | 通过 |
| Python/relay | `test_nginx_clients_have_separate_ip_budgets` | 通过 |
| Python/relay | `test_pair_bidirectional_bytes_and_cleanup` | 通过 |
| Python/relay | `test_session_limit_closes_both_peers` | 通过 |

<a id="screenshotocrtest"></a>
### ScreenshotOcrTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/ScreenshotOcrTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/ScreenshotOcrTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `bundledModelsActuallyReadChineseAndEnglishScreenshot` | 通过 |
| 共享/桌面JVM | `headerAndCreatorAbovePlaybackToolbarAreNotImportedAsMusic` | 通过 |
| 共享/桌面JVM | `numericSongTitlesArePreservedWhileRankingColumnIsIgnored` | 通过 |
| 共享/桌面JVM | `parserDoesNotInventArtistsFromEmptyOrUnpairedText` | 通过 |
| 共享/桌面JVM | `parserPairsSongsAndSkipsControlsBadgesAndOverlappingRows` | 通过 |
| 共享/桌面JVM | `publicCatalogParsingAndRelaxedSearchStillRequireTheVisibleTitleAndArtist` | 通过 |
| 共享/桌面JVM | `recognitionModeDefaultsLocallyAndRoundTripsWithoutCredentials` | 通过 |
| 共享/桌面JVM | `truncatedSongIsCompletedOnlyFromUnambiguousMatchingEvidence` | 通过 |

<a id="selectedplayliststatetest"></a>
### SelectedPlaylistStateTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/SelectedPlaylistStateTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/SelectedPlaylistStateTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `doesNotRestoreRemovedLocalLibraryCard` | 通过 |
| 共享/桌面JVM | `fallsBackToFirstPlaylistWhenStoredIdIsMissing` | 通过 |
| 共享/桌面JVM | `restoresExistingPlaylist` | 通过 |
| 共享/桌面JVM | `returnsNullForEmptyLibrary` | 通过 |

<a id="semanticsearchintentparsertest"></a>
### SemanticSearchIntentParserTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/model/SemanticSearchIntentParserTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/model/SemanticSearchIntentParserTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `parseRainyHealingAcousticQuery` | 通过 |
| 共享/桌面JVM | `parseRunningAndBpmQuery` | 通过 |
| 共享/桌面JVM | `parseSlowBalladQuery` | 通过 |

<a id="sitetest"></a>
### SiteTest

源码：[website/tests/test_site.py](../../../website/tests/test_site.py)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Python/website | `test_anonymous_post_reply_and_xss_are_safe` | 通过 |
| Python/website | `test_asset_version_matches_bytes_and_changes_with_content` | 通过 |
| Python/website | `test_author_campaign_requires_explicit_confirmation` | 通过 |
| Python/website | `test_author_can_participate_and_replies_are_pinned_for_every_reader` | 通过 |
| Python/website | `test_author_deletion_is_protected_and_cascades` | 通过 |
| Python/website | `test_author_forms_render_and_announcement_is_escaped` | 通过 |
| Python/website | `test_author_release_route_requires_both_correctly_named_packages` | 通过 |
| Python/website | `test_cleanup_never_deletes_outside_its_owned_directory` | 通过 |
| Python/website | `test_confirmation_is_bound_to_the_selected_preferences` | 通过 |
| Python/website | `test_csrf_protects_public_and_author_mutations` | 通过 |
| Python/website | `test_existing_author_replies_stay_pinned_after_restart` | 通过 |
| Python/website | `test_failed_upload_preserves_active_release_and_cleans_staging` | 通过 |
| Python/website | `test_fake_svg_image_is_rejected` | 通过 |
| Python/website | `test_home_and_forum_announcement_link_and_content` | 通过 |
| Python/website | `test_invalid_linux_asset_cannot_switch_any_platform_or_remove_the_previous_release` | 通过 |
| Python/website | `test_large_author_upload_uses_the_release_limit_not_public_limit` | 通过 |
| Python/website | `test_latest_downloads_and_range_requests` | 通过 |
| Python/website | `test_login_throttle_persists_and_does_not_store_plain_ip` | 通过 |
| Python/website | `test_low_disk_space_keeps_the_previous_release` | 通过 |
| Python/website | `test_no_smtp_means_no_false_subscription_success` | 通过 |
| Python/website | `test_one_click_unsubscribe_uses_a_valid_capability_token` | 通过 |
| Python/website | `test_owner_can_atomically_publish_three_platforms_and_reject_wrong_linux_version` | 通过 |
| Python/website | `test_owner_can_create_topic_and_delete_empty_topic_with_guard` | 通过 |
| Python/website | `test_owner_can_pin_and_unpin_post` | 通过 |
| Python/website | `test_owner_dashboard_stats_and_post_search` | 通过 |
| Python/website | `test_owner_quick_delete_post_and_reply_by_query` | 通过 |
| Python/website | `test_password_change_revokes_existing_author_session` | 通过 |
| Python/website | `test_post_survives_application_restart` | 通过 |
| Python/website | `test_promotion_campaign_only_includes_explicit_opt_in` | 通过 |
| Python/website | `test_public_pages_have_no_author_entry_or_old_slogan` | 通过 |
| Python/website | `test_public_upload_is_verified_and_reencoded_and_removed_with_post` | 通过 |
| Python/website | `test_release_switch_removes_old_packages_but_keeps_history` | 通过 |
| Python/website | `test_response_headers_and_owner_robots` | 通过 |
| Python/website | `test_subscription_needs_email_confirmation` | 通过 |
| Python/website | `test_three_platform_release_includes_linux_with_correct_links_hash_and_range` | 通过 |
| Python/website | `test_topic_request_is_private_until_approved` | 通过 |
| Python/website | `test_unsubscribe_cancels_pending_mail_and_old_confirm_link` | 通过 |
| Python/website | `test_unversioned_or_wrong_asset_versions_do_not_get_immutable_cache` | 通过 |
| Python/website | `test_versioned_public_assets_cache_without_cookies` | 通过 |
| Python/website | `test_worker_persists_attempts_and_retries_without_real_mail` | 通过 |
| Python/website | `test_worker_rechecks_opt_out_before_connecting_to_smtp` | 通过 |

<a id="smartlibraryauditenginetest"></a>
### SmartLibraryAuditEngineTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/model/SmartLibraryAuditEngineTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/model/SmartLibraryAuditEngineTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `auditCleansGarbagePrefixAndParsesArtistFromTitle` | 通过 |
| 共享/桌面JVM | `auditIdentifiesDuplicatesAndRecommendsBestQuality` | 通过 |

<a id="sourcedownloadtimeouttest"></a>
### SourceDownloadTimeoutTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/sources/SourceDownloadTimeoutTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/sources/SourceDownloadTimeoutTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `progressingAudioOutlivesMetadataDeadlineWithoutChangingPublicAddressGuards` | 通过 |

<a id="statepreservationtest"></a>
### StatePreservationTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/StatePreservationTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/StatePreservationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `draftStoreKeepsIndependentDraftsPerProvider` | 通过 |
| 共享/桌面JVM | `draftStorePreservesUnsavedInputAcrossRecreationWithoutBundleSerialization` | 通过 |

<a id="storageandassistantextratest"></a>
### StorageAndAssistantExtraTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/StorageAndAssistantExtraTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/StorageAndAssistantExtraTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `assistantPlaybackAndNavigationActionsExecuteAndRejectInvalid` | 通过 |
| 共享/桌面JVM | `cleanupRollsBackWhenFileDeleteFails` | 通过 |
| 共享/桌面JVM | `lyricOffsetStoreDirectionAndPersistence` | 通过 |
| 共享/桌面JVM | `recommendationHistoryPersistsAcrossRestartAndEnforcesRollingDays` | 通过 |

<a id="supervisortest"></a>
### SupervisorTest

源码：[services/relay/test_supervise.py](../../../services/relay/test_supervise.py)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Python/relay | `test_crashing_real_child_stops_at_budget` | 通过 |
| Python/relay | `test_missing_executable_does_not_retry_forever` | 通过 |
| Python/relay | `test_parent_retirement_terminates_live_child` | 通过 |

<a id="syncpackagedialogdevicetest"></a>
### SyncPackageDialogDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/SyncPackageDialogDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/SyncPackageDialogDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `exportScopesScrollConfirmCancelAndReopenWithoutFreezingHost` | 通过 |

<a id="syncpackagedialogtest"></a>
### SyncPackageDialogTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/SyncPackageDialogTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/SyncPackageDialogTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `emptyLibraryAndImportPreserveNullScopeAndDoNoWorkBeforeConfirmation` | 通过 |
| 共享/桌面JVM | `lastPlaylistRemainsSelectableAndActionsFitShortLargeTextWindows` | 通过 |
| 共享/桌面JVM | `openingLargeCatalogOnlyComposesVisiblePlaylistOptions` | 通过 |

<a id="syncpackagefeedbackdevicetest"></a>
### SyncPackageFeedbackDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/SyncPackageFeedbackDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/SyncPackageFeedbackDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `completionKeepsLocationVisibleWhenFileManagerCannotOpen` | 通过 |

<a id="syncpackagefeedbacktest"></a>
### SyncPackageFeedbackTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/SyncPackageFeedbackTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/SyncPackageFeedbackTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `completedExportShowsActualLocationAndLongFilenameWithReachableActions` | 通过 |
| 共享/桌面JVM | `failureNeverDisplaysSuccessOrLocationActions` | 通过 |
| 共享/桌面JVM | `progressCanBeDismissedAndBusyScreenCannotStartAnotherArchive` | 通过 |

<a id="syncpackagenamingtest"></a>
### SyncPackageNamingTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/model/SyncPackageNamingTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/model/SyncPackageNamingTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `longUnicodeNamesStayWithinFilenameLimitsWithoutCuttingEmojiInHalf` | 通过 |
| 共享/桌面JVM | `namesAreSinglePortableComponentsIncludingWindowsReservedNames` | 通过 |
| 共享/桌面JVM | `playlistTitleIsTheFilenameWithoutTimestampsAndLibraryHasAClearName` | 通过 |
| 共享/桌面JVM | `repeatedExportsKeepExistingFilesAndUseTheNextFreeSuffix` | 通过 |

<a id="tasteanalyticsenginetest"></a>
### TasteAnalyticsEngineTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/model/TasteAnalyticsEngineTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/model/TasteAnalyticsEngineTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `emptyTracksReturnsDefaultPersona` | 通过 |
| 共享/桌面JVM | `tracksWithEmotionalKeywordsProduceNostalgicOrDreamerPersona` | 通过 |

<a id="togethercompaniondevicetest"></a>
### TogetherCompanionDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherCompanionDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherCompanionDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `androidKeystoreMutualTlsChatPairAndDirectInvite` | 通过 |

<a id="togethercompaniontest"></a>
### TogetherCompanionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherCompanionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherCompanionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `boundedPendingQueueNeverSilentlyDropsUndeliveredMessages` | 通过 |
| 共享/桌面JVM | `encryptedIdentityAndHistorySurviveRestartAndClearDoesNotReplay` | 通过 |
| 共享/桌面JVM | `firstLaunchDoesNotListenUntilRoomActionButRememberedPartnersCanReceiveInvites` | 通过 |
| 共享/桌面JVM | `incomingMessageEventFiresAfterPersistenceButNotForReplayOrClearedHistory` | 通过 |
| 共享/桌面JVM | `legacyAnonymousRoomsRemainCompatibleBothWays` | 通过 |
| 共享/桌面JVM | `realTlsChatPairingLocalClearAndStableDeviceIdentityAcrossNewRooms` | 通过 |
| 共享/桌面JVM | `rememberedPartnerInviteRequiresAcceptanceAndRemovalRevokesOffers` | 通过 |

<a id="togethercompanionuidevicetest"></a>
### TogetherCompanionUiDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherCompanionUiDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherCompanionUiDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `failedLocalSaveKeepsDraftAndClearRequiresConfirmation` | 通过 |
| Android设备 | `pairedInvitationDoesNotJoinUntilRecipientAccepts` | 通过 |

<a id="togetherconnectionboundstest"></a>
### TogetherConnectionBoundsTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherConnectionBoundsTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherConnectionBoundsTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `candidateCountAndEveryAddressAreValidatedBeforeConnecting` | 通过 |
| 共享/桌面JVM | `leavingDuringParallelTlsHandshakesClosesAllAttemptsPromptly` | 通过 |

<a id="togetherconnectiondevicetest"></a>
### TogetherConnectionDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherConnectionDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherConnectionDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `androidPeersCanCreateAndValidateEphemeralTlsCertificates` | 通过 |
| Android设备 | `physicalAndroidFollowsWindowsAudioPauseSeekAndReplacement` | 条件跳过 |

<a id="togetherdevicebridgetest"></a>
### TogetherDeviceBridgeTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherDeviceBridgeTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherDeviceBridgeTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `windowsHostForPhysicalAndroidPeer` | 条件跳过 |

<a id="togethernativebridgetest"></a>
### TogetherNativeBridgeTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherNativeBridgeTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherNativeBridgeTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `windowsNativeHostForMuMu` | 条件跳过 |

<a id="togethernativeplaybackdevicetest"></a>
### TogetherNativePlaybackDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherNativePlaybackDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherNativePlaybackDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `checkAssistantAvailabilityForPromoWithoutExportingCredentials` | 条件跳过 |
| Android设备 | `mumuNativeHostKeepsPublishingAfterActivityGoesIntoBackground` | 条件跳过 |
| Android设备 | `servicePlayerFollowsWindowsAfterActivityGoesIntoBackground` | 条件跳过 |

<a id="togetherplaybackdrivertest"></a>
### TogetherPlaybackDriverTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/TogetherPlaybackDriverTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/TogetherPlaybackDriverTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `interruptionWhilePreparingNeverResumesOrAcknowledgesTheOldAudio` | 通过 |
| 共享/桌面JVM | `localAudioInterruptionKeepsRoomButNeverAutoplaysUntilExplicitResume` | 通过 |
| 共享/桌面JVM | `rapidReplacementNeverStartsOrAcknowledgesAnOldPreparedTrack` | 通过 |
| 共享/桌面JVM | `smallTimingNoiseDoesNotSeekAndLargeCorrectionsHaveHysteresis` | 通过 |

<a id="togetherprefetchcancellationtest"></a>
### TogetherPrefetchCancellationTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherPrefetchCancellationTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherPrefetchCancellationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `networkChangeOrExplicitDisableRetiresPartialPrefetchWithoutDroppingCurrentSong` | 通过 |

<a id="togetherrendezvoustest"></a>
### TogetherRendezvousTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherRendezvousTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherRendezvousTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `javaSignaturesInteroperateAndChangedPartnerEndpointCanBeInvited` | 通过 |
| 共享/桌面JVM | `productionUrlsRequireHttpsWithoutCredentialsQueriesOrFragments` | 通过 |

<a id="togetherreversenativebridgetest"></a>
### TogetherReverseNativeBridgeTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherReverseNativeBridgeTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherReverseNativeBridgeTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `windowsNativeGuestFollowsMuMuHost` | 条件跳过 |

<a id="togetherserverroutingtest"></a>
### TogetherServerRoutingTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherServerRoutingTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherServerRoutingTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `serverFailureDoesNotDialReachablePeerAddress` | 通过 |
| 共享/桌面JVM | `serverRoomDoesNotPublishPeerNetworkAddresses` | 通过 |

<a id="togethersessiontest"></a>
### TogetherSessionTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherSessionTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherSessionTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `dynamicMeteredNetworkPreventsAndCancelsPrefetch` | 通过 |
| 共享/桌面JVM | `evictedPreviousTrackIsRequestedAgainWithoutLosingCurrentAudio` | 通过 |
| 共享/桌面JVM | `hostApprovalPrecedesAnyAudioReadAndCanReject` | 通过 |
| 共享/桌面JVM | `invitationInspectionAndQrAreLocalStrictAndRoundTrip` | 通过 |
| 共享/桌面JVM | `lyricsArriveBoundedWithoutExposingHostTrackIdsOrPaths` | 通过 |
| 共享/桌面JVM | `prefetchedNextTrackIsVerifiedAndReusedWithoutAnotherRead` | 通过 |
| 共享/桌面JVM | `refreshedAndExpiredInvitationsCannotStartTransfers` | 通过 |
| 共享/桌面JVM | `sharedQueueUsesOpaqueIdsAndMembersCanControlWithoutApproval` | 通过 |
| 共享/桌面JVM | `temporaryNetworkBreakRecoversWithSeparateCredential` | 通过 |
| 共享/桌面JVM | `thirtyJoinLeaveCyclesRetireOwnedFilesAndPreserveOtherCacheFiles` | 通过 |

<a id="togethersuggestionstest"></a>
### TogetherSuggestionsTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/TogetherSuggestionsTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/TogetherSuggestionsTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `indicesReferOnlyToTheCapturedCandidateList` | 通过 |
| 共享/桌面JVM | `malformedInventedAndDuplicateCandidatesAreRejected` | 通过 |

<a id="togetheruidevicetest"></a>
### TogetherUiDeviceTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherUiDeviceTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherUiDeviceTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `externalInvitationShowsAddressWithoutAutomaticallyConnecting` | 通过 |
| Android设备 | `incomingFriendMustBeApprovedAndControlsRemainAccessible` | 失败 |
| Android设备 | `openingOrPastingAnInvitationDoesNotConnectUntilExplicitJoin` | 失败 |
| Android设备 | `roomCreationAndClosureUseExplicitActionsAndKeepPortVisible` | 失败 |

<a id="togetherwebsockettest"></a>
### TogetherWebSocketTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherWebSocketTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherWebSocketTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `defaultsRespectSavedOptOutAndCustomServer` | 通过 |
| 共享/桌面JVM | `deployedPinnedTlsRoundTripOverWebSocket` | 条件跳过 |
| 共享/桌面JVM | `endpointRejectsInsecureOrAmbiguousTargets` | 通过 |
| 共享/桌面JVM | `realPinnedTlsRoundTripOverWebSocket` | 通过 |

<a id="tutorialnavigationtest"></a>
### TutorialNavigationTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TutorialNavigationTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TutorialNavigationTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `offscreenSettingsTargetIsBroughtIntoView` | 通过 |
| Android设备 | `pauseAndResumeRetainStepAndDoNotEnableRecommendations` | 通过 |
| Android设备 | `quickGuideRemainsNavigableWithLargeTextAndShortViewport` | 通过 |
| Android设备 | `settingsOpensQuickGuideAndAllFourPagesCanBeRead` | 失败 |
| Android设备 | `spotlightPassesRealClicksAndAdvancesThroughPlayerTabs` | 通过 |
| Android设备 | `welcomeCanBeSkippedAndIsNotOfferedAgain` | 失败 |

<a id="tutorialprogresstest"></a>
### TutorialProgressTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/features/TutorialProgressTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/TutorialProgressTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `invalidAndFutureProgressCannotEscapeKnownChapterBounds` | 通过 |
| 共享/桌面JVM | `missingTrackRoutesToLibraryWithoutExecutingAnOperation` | 通过 |
| 共享/桌面JVM | `pauseRestoresExactStepAndOtherChaptersRemainIndependent` | 通过 |
| 共享/桌面JVM | `tutorialPreferenceUsesAtomicUiStoreAndPreservesPlaybackSettings` | 通过 |
| 共享/桌面JVM | `welcomeIsOnlyOfferedUntilDismissedAndSurvivesRestart` | 通过 |
| 共享/桌面JVM | `wrongEventsCannotCompleteStepsAndSkippingDoesNotPretendSuccess` | 通过 |

<a id="uiinteractioncontracttest"></a>
### UiInteractionContractTest

源码：[composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/UiInteractionContractTest.kt](../../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/UiInteractionContractTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| Android设备 | `albumAtmosphereCanReadNativeArtworkPixels` | 通过 |
| Android设备 | `seekAndVolumeExposeNativeProgressActionsAndFreshCallbacks` | 通过 |
| Android设备 | `tabsExposeSelectionAndKeepTouchTargetsInReducedMotion` | 通过 |

<a id="uivisualcontracttest"></a>
### UiVisualContractTest

源码：[composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt](../../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `albumAtmosphereFollowsColoredArtworkAndIgnoresTransparentArt` | 通过 |
| 共享/桌面JVM | `assistantAttachmentsOnlyActOnExplicitClicks` | 通过 |
| 共享/桌面JVM | `bufferingAndLoadingStatesRenderDistinctVisualFeedbackInDarkAndLight` | 通过 |
| 共享/桌面JVM | `compactAssistantKeepsTitleOnOneLineWithLargeText` | 通过 |
| 共享/桌面JVM | `draggingProgressCommitsOnlyOnceOnRelease` | 通过 |
| 共享/桌面JVM | `emptyLibraryKeepsEntryActionsUsableWithLargeText` | 通过 |
| 共享/桌面JVM | `galleryPlayerKeepsTransportReachableWithLargeTextInShortWindow` | 通过 |
| 共享/桌面JVM | `nativePagesReverseWithoutRecreatingTheDepartingContent` | 通过 |
| 共享/桌面JVM | `nativeSlidersAndTabsExposeWorkingActionsAndMinimumTargets` | 通过 |
| 共享/桌面JVM | `playbackErrorOffersRetryInBothThemes` | 通过 |
| 共享/桌面JVM | `playbackWaveMovesOnlyWhilePlayingAndKeepsNativeSeek` | 通过 |
| 共享/桌面JVM | `renderApprovedLightNativePagesAndSettings` | 通过 |
| 共享/桌面JVM | `renderApprovedPlayerPanes` | 通过 |
| 共享/桌面JVM | `renderFrostedDestinationsThroughTheRealShell` | 通过 |
| 共享/桌面JVM | `shortLandscapeKeepsPaneTabsAndTransportInsideViewport` | 通过 |
| 共享/桌面JVM | `shortLandscapeRetainsCollectionViewportAndTransport` | 通过 |
| 共享/桌面JVM | `songInformationRemainsReachableFromPlayerMenu` | 通过 |
| 共享/桌面JVM | `stageBackdropRemainsStableAcrossPlaybackAndAccessibilityStates` | 通过 |
| 共享/桌面JVM | `switchingThemeKeepsFirstFrameReadable` | 通过 |
| 共享/桌面JVM | `textAndSelectedRolesStayReadableAcrossAllThemes` | 通过 |
| 共享/桌面JVM | `wideStageKeepsLyricsQueueAndTransportConnected` | 通过 |

<a id="weatherpalettetest"></a>
### WeatherPaletteTest

源码：[composeApp/src/commonTest/kotlin/com/resonance/player/WeatherPaletteTest.kt](../../../composeApp/src/commonTest/kotlin/com/resonance/player/WeatherPaletteTest.kt)

| 执行环境 | 用例 | 结果 |
| --- | --- | --- |
| 共享/桌面JVM | `ambiguousWeatherPreservesLastPalette` | 通过 |
| 共享/桌面JVM | `clearWeatherUsesSage` | 通过 |
| 共享/桌面JVM | `invalidLocationsCannotBeUsed` | 通过 |
| 共享/桌面JVM | `rainSnowAndStormsUseBlue` | 通过 |

## 未完成与主动排除

- 首轮整组 `UiVisualContractTest` 超过150秒；拆为22个方法后21个通过，`renderPhoneDesktopPlayerAndToolsWithFixtureData` 仍超过90秒，只完成66/70个截图输出，方法最终断言未确认。没有把已落盘图片当成该测试通过。
- `renderNativeMotionPreviewFrames` 会生成360帧视频素材，本轮主动排除；播放页和歌词的独立交互/中间帧断言仍执行。
- 条件跳过的公网、收费AI、Windows/手机原生桥接与宣传捕获项目见上表。
- 手机首轮 `UiInteractionContractTest` 在返回用例结果前报告Process crashed；同包同源码单独重跑3项通过，有效结果只计一次，原始异常与10项失败分析见[Android续测报告](ANDROID-DEVICE-REPORT.md)。
