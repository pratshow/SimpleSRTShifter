# SimpleSRTShifter
A simple java program to bulk shift SRT subtitles files forward or back.

## Usage
`javac SRTShift.java`

`java SRTShift <input_directory> <output_directory> <seconds_to_shift> <pause_length>`

Parameters

    <input_directory>: The directory containing the SRT files to be processed.
    <output_directory>: The directory where the adjusted SRT files will be saved.
    <seconds_to_shift>: The number of seconds to shift the subtitles. Use positive numbers to shift forward and negative numbers to shift backward. Decimal values are allowed (e.g., -9.5).
    <pause_length>: The length of pause (in seconds) to identify long pauses in the subtitles. This is to not apply the syncing to subtitles that are before the pause and only apply them after the pause. Use 0 to ignore this parameter.

### Example

`java SRTShift /path/to/input /path/to/output -5.0 30.0`

This example shifts all subtitles in the input directory backward by 5 seconds for all subtitles after the 30+ second pause.
The pause pause_length parameter is intened to be used in the case when the subtitles are in sync normally, but get off sync after something like an Opening plays.
This allows the subtitles to keep the correct pre-intro synchronization. But then properly sync the errant post-opening subtitles.
