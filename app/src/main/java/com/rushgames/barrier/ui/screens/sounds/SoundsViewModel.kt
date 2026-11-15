package com.rushgames.barrier.ui.screens.sounds

import androidx.lifecycle.ViewModel
import com.rushgames.barrier.sound.SoundPlayer
import com.rushgames.barrier.sound.SoundType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class SoundCategory { NATURE, AMBIENT, NOISE }

data class AmbientSound(
    val id: String,
    val name: String,
    val emoji: String,
    val category: SoundCategory,
    val soundType: SoundType,
    val volume: Float = 0.7f,
    val isPlaying: Boolean = false
)

data class SoundsUiState(
    val sounds: List<AmbientSound> = defaultSounds(),
    val isMixMode: Boolean = false,
    val masterVolume: Float = 0.8f
)

fun defaultSounds() = listOf(
    AmbientSound("rain", "Rain", "🌧", SoundCategory.NATURE, SoundType.RAIN),
    AmbientSound("ocean", "Ocean Waves", "🌊", SoundCategory.NATURE, SoundType.OCEAN),
    AmbientSound("forest", "Forest", "🌿", SoundCategory.NATURE, SoundType.NATURE),
    AmbientSound("coffee_shop", "Coffee Shop", "☕", SoundCategory.AMBIENT, SoundType.COFFEE_SHOP),
    AmbientSound("library", "Library", "📚", SoundCategory.AMBIENT, SoundType.LIBRARY),
    AmbientSound("office", "Office", "🖥", SoundCategory.AMBIENT, SoundType.OFFICE),
    AmbientSound("white_noise", "White Noise", "〰", SoundCategory.NOISE, SoundType.WHITE_NOISE),
    AmbientSound("brown_noise", "Brown Noise", "🟤", SoundCategory.NOISE, SoundType.BROWN_NOISE)
)

@HiltViewModel
class SoundsViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoundsUiState())
    val uiState: StateFlow<SoundsUiState> = _uiState.asStateFlow()

    fun toggleSound(id: String) {
        _uiState.update { state ->
            val targetSound = state.sounds.find { it.id == id } ?: return@update state
            val willPlay = !targetSound.isPlaying

            if (willPlay) {
                soundPlayer.playSound(targetSound.soundType, targetSound.volume * state.masterVolume)
            } else {
                soundPlayer.stopSound()
            }

            val updated = state.sounds.map { sound ->
                if (sound.id == id) {
                    sound.copy(isPlaying = willPlay)
                } else if (!state.isMixMode) {
                    sound.copy(isPlaying = false)
                } else {
                    sound
                }
            }
            state.copy(sounds = updated)
        }
    }

    fun setVolume(id: String, volume: Float) {
        _uiState.update { state ->
            val updated = state.sounds.map {
                if (it.id == id) {
                    if (it.isPlaying) soundPlayer.setVolume(volume * state.masterVolume)
                    it.copy(volume = volume)
                } else it
            }
            state.copy(sounds = updated)
        }
    }

    fun toggleMixMode() = _uiState.update { it.copy(isMixMode = !it.isMixMode) }

    fun setMasterVolume(v: Float) {
        _uiState.update { state ->
            soundPlayer.setVolume(v)
            state.copy(masterVolume = v)
        }
    }

    fun stopAll() {
        soundPlayer.stopSound()
        _uiState.update { state ->
            state.copy(sounds = state.sounds.map { it.copy(isPlaying = false) })
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.stopSound()
    }
}
