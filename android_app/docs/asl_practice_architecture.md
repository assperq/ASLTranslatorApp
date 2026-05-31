# ASL practice and gesture-recognition quiz architecture

## Feature 1: practice after text translation

`TranslatorViewModel` keeps text translation independent from practice by delegating session progress to `PracticeSessionManager`. The manager stores the normalized letter sequence, current index, error count, start/end timestamps, progress, and final statistics. The existing live-camera recognition path remains unchanged until a stabilized prediction is accepted; then `TranslatorViewModel` routes the stable letter either to translation output or to practice validation depending on `PracticeUiState.mode`.

## Feature 2: gesture-recognition quiz

`GestureRecognitionQuizViewModel` owns quiz state and CameraX binding for the new screen, but it reuses the existing `RecognitionManager`, `GestureRecognitionEngine`, `HandLandmarkerHelper`, `AslClassifier`, and `PredictionStabilizer` pipeline. A question is completed only after the stabilizer produces the requested letter. Wrong predictions increase mistake counters and keep the same question active.

## Component diagram

```plantuml
@startuml
package "Translator" {
  class TranslatorViewModel
  class TranslatorUiState
  class PracticeUiState
  class PracticeSessionManager
  class TranslationPanel
}

package "Gesture quiz" {
  class GestureRecognitionQuizViewModel
  class GestureRecognitionQuizUiState
  class GestureRecognitionQuizScreen
}

package "Recognition pipeline" {
  class RecognitionManager
  class HandLandmarkerHelper
  class GestureRecognitionEngine
  class AslClassifier
  class PredictionStabilizer
}

TranslatorViewModel --> TranslatorUiState
TranslatorUiState --> PracticeUiState
TranslatorViewModel --> PracticeSessionManager
TranslatorViewModel --> RecognitionManager
TranslatorViewModel --> PredictionStabilizer
TranslationPanel --> PracticeUiState

GestureRecognitionQuizScreen --> GestureRecognitionQuizViewModel
GestureRecognitionQuizViewModel --> GestureRecognitionQuizUiState
GestureRecognitionQuizViewModel --> RecognitionManager
GestureRecognitionQuizViewModel --> PredictionStabilizer

RecognitionManager --> HandLandmarkerHelper
RecognitionManager --> GestureRecognitionEngine
GestureRecognitionEngine --> AslClassifier
@enduml
```

## Practice sequence

```plantuml
@startuml
actor User
participant TranslationPanel
participant TranslatorViewModel
participant PracticeSessionManager
participant RecognitionManager
participant PredictionStabilizer
participant "HandLandmarkerHelper + AslClassifier" as Pipeline

User -> TranslationPanel: Tap "Start practice"
TranslationPanel -> TranslatorViewModel: startPractice()
TranslatorViewModel -> PracticeSessionManager: start(textInput)
TranslatorViewModel -> TranslatorViewModel: bind live camera
loop camera frames
  TranslatorViewModel -> RecognitionManager: detect(imageProxy)
  RecognitionManager -> Pipeline: landmarks + classification
  RecognitionManager --> TranslatorViewModel: RecognitionResult?
  TranslatorViewModel -> PredictionStabilizer: add/resolve(letter)
  alt stable letter
    TranslatorViewModel -> PracticeSessionManager: submitPrediction(letter)
    alt correct
      PracticeSessionManager --> TranslatorViewModel: Correct(snapshot)
      TranslatorViewModel -> TranslationPanel: next letter / finished stats
    else wrong
      PracticeSessionManager --> TranslatorViewModel: Wrong(snapshot)
      TranslatorViewModel -> TranslationPanel: show error, keep current letter
    end
  end
end
@enduml
```

## Gesture-recognition quiz sequence

```plantuml
@startuml
actor User
participant GestureRecognitionQuizScreen as Screen
participant GestureRecognitionQuizViewModel as ViewModel
participant RecognitionManager
participant PredictionStabilizer
participant "HandLandmarkerHelper + AslClassifier" as Pipeline

User -> Screen: Start quiz
Screen -> ViewModel: start()
ViewModel -> ViewModel: choose random ASL letter
ViewModel -> Screen: UiState(currentLetter)
loop camera frames
  ViewModel -> RecognitionManager: detect(imageProxy)
  RecognitionManager -> Pipeline: landmarks + classification
  RecognitionManager --> ViewModel: RecognitionResult?
  ViewModel -> PredictionStabilizer: add/resolve(letter)
  alt stable prediction equals current letter
    ViewModel -> ViewModel: score++, nextQuestionOrFinish()
    ViewModel -> Screen: success + next letter/result
  else stable prediction is different
    ViewModel -> ViewModel: mistakes++, keep current question
    ViewModel -> Screen: error, try again
  end
end
@enduml
```
