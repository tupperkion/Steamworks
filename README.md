# Jeffrey `v3.0`

The latest version of the Jeffrey robot code changes the function of the code at a fundamental level. It takes what wpilib already provides with command-based programming, and builds on top of it adding a swarm of new features. With that said, it is highly recommended that any new programmers read the following sections of the wpilib documentation:

 1. [Using actuators (motors, servos, and relays)](https://wpilib.screenstepslive.com/s/4485/m/13809/c/88897)
 2. [Driver Station Inputs and Feedback](https://wpilib.screenstepslive.com/s/4485/m/13809/c/88894)
 
And most importantly...

 3. [**Command based programming**](https://wpilib.screenstepslive.com/s/4485/m/13809/c/88893)
 
Starting [here](https://wpilib.screenstepslive.com/s/4485/m/13809) and reading more about sensors and other things will also serve you well, but the 3 items above are the most important.

# Setting up a workspace

As of now, we don't use Eclipse anymore. We use a combination of [Intellij IDEA](https://www.jetbrains.com/idea/) for the development environment and [GradleRIO](https://github.com/Open-RIO/GradleRIO) for the workspace.

## Getting started

Make sure you have IntelliJ and Java 8 installed. GradleRIO is already built-in to this repository. Clone this repository and open a terminal in the root folder. Then just run `./gradlew idea` (`gradlew idea` if you are on windows) and wait for GradleRIO to do the magic. You should also talk to the programming captain about importing settings as well.

## Pushing code to the robot

### `./gradlew build deploy --offline`

That's the magic command. The nice thing about GradleRIO is you don't even have to load up your IDE to push code. However, in case you do have IntelliJ open, it is possible to set up the "Run code" button to do this. We won't get into that here yet, but you can talk to your friendly neighborhood programming captain for more information.

# Programming!

This project builds on top of the [command-based programming](https://wpilib.screenstepslive.com/s/4485/m/13809/c/88893) already present in wpilib. This documentation assumes you have already read that section and are reasonably familiar with it. This simply discusses what is _different_ between our code and what is documented there.

## Scheduler

The Midcoast Maineiacs Scheduler is similar to the wpilib Scheduler, with some added benefits:

 - The Scheduler can be enabled or disabled by the code; it is safe to assume that `Scheduler.enabled = false` will be equivalent to the robot being fully disabled. At the moment, `Scheduler.enabled` simply reflects the mobility of the robot as indicated by the RSL light, but other special conditions can be added as new features as need be.
 - When the Scheduler is "disabled," commands are simply frozen if `willRunWhenDisabled` is false. They will _not_ be cancelled by the Scheduler, however `Robot` may cancel them in some cases.
 - The Scheduler keeps track of which command it is running, so it is easy to see what command has led to a specific code segment being called via the use of `Scheduler.getCurrentCommand()`.
 - It also runs using a `Timer` on a separate thread with exact 50Hz timing, meaning it is safe to use the iteration of commands for timing. WPILib traditionally can vary the timing based on the behavior of the network connection, but the Timer is independent of the network.
 - Because we have full control of the Scheduler, we can modify it easily to implement other features. This probably won't be the last time you hear mention of the Scheduler.

Also, the `isFinished()` value is checked twice for every iteration. Once before executing (unless the command hasn't started yet, it is guaranteed that `initialize` and `execute` will be called exactly once before `isFinished` is checked), and once after executing. See order below:

 1. If this _isn't_ the first run, check `isFinished()`; stop command if true
 2. Check `isCancelled()`; stop command if true
 3. If this _is_ the first run, run `initialize()`
 4. Run `execute()`
 5. Check `isFinished()`; stop command if true
 6. Check `isCancelled()`; stop command if true
 7. 20ms (minus the time it took to process the command) delay

The Scheduler class extends `TimerTask` and can be instantiated and used for creating a `Timer`. This is already done by `Robot`. _All_ methods and variables (except for `run()`, which makes it a valid `TimerTask`) are static.

Commands are started by `Scheduler.add(Command)`. `Command.start()` should work for all commands still, but some commands may need to have their `start()` methods manually changed to use `add()` (see [starting commands](#starting-commands) below).

## Subsystems

All subsystems extend an `MMSubsystem` class, which extends the WPILib `Subsystem` class. This expands upon the normal WPILib subsystem, adding new features:

 - All subsystems can be "controlled" by a command. The most important thing to note here is when you write your subsystems, make sure the methods that actually actuate the motors check that `verifyResponse()` returns `true` before doing anything. The `verifyResponse()` makes sure that if the method was called by a command, that command actually has control of the subsystem, and if it wasn't called by a command, the subsystem isn't currently being controlled by a command.
 - All subsystems have a `stop()` method. This is abstract in `MMSubsystem`, which means when you make a new subsystem, IntelliJ will scream at you until you override `stop()`. This method should unconditionally stop all actuators, without checking `verifyResponse`.
 - `MMSubsystem` also provides a `controlledByTeleop()` method which returns true when teleop is enabled (according to the Scheduler) and the subsystem is currently _not_ controlled by any command. This implies that the subsystem should be responding to controller inputs from the driver station.
 
> If this doesn't make sense, just keep reading.

## Commands

Commands are probably the most complicated part of this project (except maybe DriveTrain, but that will vary year-to-year). There are two types of commands, _active_ commands and _passive_ commands. _Active_ commands all extend the new `MMCommand` class (which extends the WPILib `Command` class), while _passive_ commands all extend the WPILib `Command` class and **not** `MMCommand`.

The primary difference between active and passive commands is that passive commands are _not_ allowed to...

 - move (or cause to move) any subsystem motors (enforced by `verifyResponse`)
 - take control of a subsystem (the `takeControl()` method only accepts `MMCommand` as an argument)
 - start _any active_ command (they can still start other passive commands, enforced by `Scheduler.add()`)

> Trying to do any of these will either result in your code crashing, or the code not compiling in the first place.

At the time of writing, `Notifier` is the only passive command. All other commands (including ones like `Series`) are active.

### Starting commands
 
Commands are started by `Scheduler.add()`, as mentioned above. `MMCommand` overrides `start()`, causing it to automatically use `Scheduler.add()`. Because active commands extend `MMCommand`, no more work needs to be done there. Passive commands, however, will by default use WPILib's `start()` method which will simply do nothing with our Scheduler. When programming passive commands, make sure you override `start()` so that it just uses the correct Scheduler. See `Notifier` for an example.

When that's said and done, commands can be started like normal with `command.start()`.

### Active commands

Active commands extend `MMCommand` and can do all of the things listed above that passive commands cannot do. These are the "do something" commands.

> **Note:** There is *no* Teleop command. Teleop controls are handled by `Robot.teleopPeriodic()`.

The `MMCommand` class adds a host of new features:

 - When one command starts another, commands will keep track of what command started them (their parents) and what commands they start (their children).
 - Commands can take control of subsystems. When a command has control, this guarantees that nothing but that command can take control of the subsystem. When a command has control of a subsystem, that implies that all of it's children also have control. It is recommended that if a command will start a command that will move a subsystem, that the parent command claims control. A child command will not take control away from its parent. It is recommended to take control during the `initialize` phase with something like `driveTrain.takeControl(this)`.
 - `MMCommand` provides a `shouldCancel()` method which checks if the Robot kill switch (the `A` button at the time of writing) or a command's parent has stopped running.
 - There is a default `isFinished()` implementation that checks if the command has timed out or if `shouldCancel()` returns true. It is recommended that if you are writing a command that controls a subsystem, you have isFinished return true of `super.isFinished` (or `shouldCancel`, depending on your needs) returns true or if you have lost control of any of the subsystems you need. E.g. `return driveTrain.controlledBy(this) || super.isFinished()`.
 - There are two timing functions available. `setTimeout()` is just WPILib's default implemantation. `timeout()` is a custom implementation that uses `setTimeout()`, but the timer will pause when the command is frozen (e.g. when the Scheduler is disabled).
 - Sometimes, you will want to start a command as your last child command, and then stop running after that command ends. This can be done by starting the command, then calling `releaseForChildren()`. When `releaseForChildren()` is called, a flag is set, which causes `shouldCancel()` to return true if there are no still-running child commands.
 - There is a new `resume()` method. This can be overridden, and is called whenever a command is frozen, then unfrozen. It should be assumed that all subsystems have had their `stop()` method called since the last time the command has had a chance to move them. Any subsystem instructions that were given during `initialize()` that would be cancelled by `stop()` (and not re-run by `execute()`) should be repeated by overriding this method.
 - There is a default `end()` implementation that will find any subsystems that this controls, and relinquish control, stopping the subsystem. It is recommended that if your command at any point takes control of another subsystem, that you either leave this method as-is or call `super.end()` when overriding it.

### `Series`

A `Series` is an active command that simply runs each argument it given in it's constructor in parallel. It is an _active_ command, as it has the ability to start other active commands. It replaces the functionality of `CommandGroup`.

It will not take control of a subsystem, but can inherit control from its parents and will pass that control down to its children. This is the normal behavior of active commands.

It will stop running once all of its children have finished running, and will not start a command before the previous one has finished. All commands will always be executed in order of how they are specified in the arguments.

There is a `Series.Parallel` class which behaves similarly to the `Series`, but it will run all commands at the same time, and stop itself once all commands have finished. A `Series` can be fed into a `Parallel` and vice versa. The `Parallel` class extends `Series`, which extends `MMCommand`.

> `Series` and `Parallel` can handle passive commands, active commands, or a combination of the two.

### `DriveCommand`

The `DriveCommand` is a convenient way of moving the drive train in a particular manner. Currently there are two forms of the constructor. One form accepts two doubles and will driving straight (using the gyro to maintain heading) at a set speed for a set period of time. The other form accepts three doubles and will set each side of the drive train to an individually specified speed for a set period of time. The timing uses the custom `timeout()` method, meaning it will be paused when the command is frozen.

## Drive train

The `DriveTrain` is a subsystem that extends `MMSubsystem`, and adds some other features.

### Autopilot

The drive train provides a `setAutopilot` function which **must** be called by an _active_ command. This function will automatically hand over control to whatever command called it, and put the drive train into autopilot mode. In this mode, the Scheduler calls the drive train's `updateAutopilot` function which will follow the instructions given by `setAutopilot`. The autopilot mode will be cancelled when either A) the command dies or B) `setAutopilot` is called with different parameters. As of the time of writing, the autopilot will simply drive in a straight line, using the gyro to maintain a straight heading. More features may be added in the future.

What sets this apart from normal control is the command doesn't actually do anything to move the motors. In fact, there doesn't even need to be an `execute` block for a command to take advantage of autopilot. All the command has to do is stay alive for however long it wants the autopilot to run, and stop running when it wants autopilot to stop. A simple use of this is to simply call `setAutopilot()` and then `timeout()` in the `initialize()` method, and let the autopilot do it's thing and let the default implementation of `isFinished()` provided by `MMCommand` to automatically stop when the timeout ends. The Scheduler will take care of constantly updating the autopilot feature to allow the drive train to do it's job.

One form of `DriveCommand` uses autopilot to stay straight.

> Autopilot will be frozen when the scheduler is disabled and will restart unhindered when the scheduler resumes, assuming the command is still running. It is not necessary to add a `resume()` method to handle autopilot.

### States

The drive train implements a state system with very complicated-looking code. It simply implements the following four states (from lowest to highest priority).

 1. `DISABLED` means the drive train is currently not doing anything
 2. `TELEOP` means the drive train is currently being controlled by the teleop controls in `teleopPeriodic()`
 3. `COMMAND` means the drive train is being controlled by a command (that is _not_ using autopilot)
 4. `AUTOPILOT` means the drive train is being controlled by its autopilot functionality

`DriveTrain` overrides `takeControl`, `relinquishControl` and other methods in order to maintain this state system. The drive train will call `Robot.notifyDriver()` (see below) any time the state changes **and** the change is _not_ simply between `COMMAND` and `AUTOPILOT`.

> **NOTE:** This does not reflect the actual state of the robot. This is what the drive train _thinks its doing_.
>
> E.g. if the auto command ends before the end of the autonomous period, the drive train will switch to the `DISABLED` state, even though the robot is still enabled.
>
> If there is an autonomous command running and the robot is disabled (and the command is frozen, not cancelled), the drive train will stay in the `COMMAND` or `AUTOPILOT` state. This doesn't mean it's moving (or trying to move), it just means it's still under the control of a command.

### Acceleration curve

The drive train also has an acceleration curve that takes effect whenever `driveLeftCurved` or `driveRightCurved` is used. It mitigates sudden acceleration by limiting how much the motor speed can change each time the method is called.

## Robot (main class)

The `Robot` class is the main class that ties everything together. It also contains the functionality formerly present in the `Teleop` and `OI` classes. It...

 - instantiates all subsystems
 - calibrates sensors
 - manages the SmartDashboard
 - instantiates and starts the Scheduler
 - ensures that the Scheduler is put in the right state (`enabled` and `teleop` statuses) whenever the robot state changes
 - resets sensors at the beginning of the match
 - detects whether or not a match is in progress (or if the robot is just being enabled or disabled manually)
 - makes sure that commands are cancelled if they need to be when the robot is disabled
	 - **NOTE:** This _does not_ happen in competition mode. This means that at competition, if the autonomous routine is still running at the end of auto, it will keep running throughout teleop.
 - handles all teleop controls
 - handles when to take control away from the autonomous commands and hand it over to the driver
 - starts and stops high-level autonomous commands

### `Notifier`

The `Notifier` is a (at the time of writing, the only) passive command in the project. Any time it is started, if there isn't already a `Notifier` running, it will send two rumble pulses to the controller to notify the driver. If there already is one running, it will cancel the duplicate `Notifier` and simply add one more pulse to the old one.

There is a static `Robot.notifyDriver()` method which simply creates and starts a `Notifier`. This method will return immediately, even before the notifier finishes, and as such, will not interrupt the flow of the code calling it.

> The `Notifier` is a passive command that _is_ set to run when disabled, and will be fully functional no matter what state the robot is in.

# Checklists for adding new features

## Subsystems

- [ ] Add public instance variables for your actuators and sensors (instantiate them directly in the subsystem class in the same way `DriveTrain` does)
- [ ] Add public methods for controlling your subsystem (make sure you check `verifyResponse`)
- [ ] Add a public `stop()` method which stops all actuators (don't check `verifyResponse`)
- [ ] Test!

## Commands

- [ ] Decide whether a command is active or passive
- [ ] If it is active and it needs to control a subsystem, make sure `initialize()` takes control of the subsystem and `isFinished()` returns true if it loses control of the subsystem
- [ ] If it uses the drive train autopilot, make sure you do that in `initialize()`
- [ ] If it is active, make sure `isFinished()` returns true if at least `shouldCancel()` (if not `super.isFinished()`) is true
- [ ] If it is _passive_, make sure to override `start()` to contain `Scheduler.add(this)`
- [ ] Test!

## Teleop controls

- [ ] Find out where to put the new controls in `teleopPeriodic()`. If there are already controls for that subsystem, then find the `if (subsystem.controlledByTeleop()) {` block for that subsystem. Otherwise, make one (make sure it's after the `if (!Scheduler.enabled) return true;` line).
- [ ] Code the controls. Check the mappings to make sure it won't conflict with other controls and make any modifications necessary. The `joystick` variable is what you need for the joystick, and the subsystems are also stored as `Robot` variables. Anywhere where you call `getRawAxis(number)` or `getRawButton(number)`, put in a command that specifies what button or axis that number is referring to.
- [ ] Update the mappings to reflect your changes.
- [ ] Test!

# Auto

The auto routine at the time of writing for the 2017 _STEAMWORKS_ challenge has three modes. The auto routine is handled by the `Auto` command and the mode is specified by an enumerator containing the possible modes.

 - `PLAY_DEAD` does nothing. It's just ignored.
 - `GEAR` runs the `Gear` command. It will drive at 50% power for half of a second before running the command if the robot starts on one of the side positions.
 - `SURGE` drives at 60% power for 2 seconds if the robot starts on one of the side positions. It will do nothing in the middle.

`Auto` and `Gear` demonstrate good use of `Series`, `DriveCommand`, and `releaseForChildren()`. Look at them for examples of usage of these features.

# Removed classes

If you have looked at some older code or code for previous years, you may have noticed a few extra classes that have been merged elsewhere. Here's the three big ones:

 - `Teleop` is no longer a command. All of the code that used to be in the `Teleop` command(s) is now in `Robot.teleopPeriodic()`
 - `RobotMap` is no longer used. It had no purpose. The motor controllers and sensors are now instantiated by the subsystems themselves.
 - `OI` is also no longer used. The joysticks are now completely handled within the `Robot` class. `Robot.joystick` is a lot nicer than `Robot.oi.getDriverJoystick()`, especially since now that the teleop controls are handled in `teleopPeriodic`, simply `joystick` is usually sufficient.
