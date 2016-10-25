package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.Servo;

@Autonomous(name="AutoRedA", group="LinearOpMode")

public class AutoRedA extends LinearOpMode {

    Servo feeder;
    DcMotor catapult;
    DcMotor paddle;
    DcMotor lDrive1;
    DcMotor lDrive2;
    DcMotor rDrive1;
    DcMotor rDrive2;
    GyroSensor gyroSensor;

    // Function to set up the Gyro
    // Function called in the init
    // Calibrates and does other preparations for the gyro sensor before autonomous
    // Needs nothing passed to it
    private void setUpGyro() throws InterruptedException {
        // setup the Gyro
        // write some device information (connection info, name and type)
        // to the log file.
        hardwareMap.logDevices();
        // get a reference to our GyroSensor object.
        gyroSensor = hardwareMap.gyroSensor.get("gyro");
        // calibrate the gyro.
        gyroSensor.calibrate();
        while (gyroSensor.isCalibrating())  {
            sleep(50);
        }
        // End of setting up Gyro
    }

    public void moveMotors(double left, double right, long time) throws InterruptedException {
        rDrive1.setPower(right);        // moves forward at certain power...
        rDrive2.setPower(right);
        lDrive1.setPower(left);
        lDrive2.setPower(left);
        sleep(time);               // ...until it's been running for a certain time(milliseconds have been multiplied by 1000 so that the result values are in seconds).
        rDrive1.setPower(0);            // at that point, the robot stops...
        rDrive2.setPower(0);
        lDrive1.setPower(0);
        lDrive2.setPower(0);
        sleep(500);                     // ...and waits a half second.
    }

    // Function to use the gyro to do a spinning turn in place.
    // It points the robot at an absolute heading, not a relative turn.  0 will point robot to same
    // direction we were at the start of program.
    // Pass:
    // targetHeading = the new heading we want to point robot at,
    // maxSpeed = the max speed the motor can run in the range of 0 to 1
    // direction = the direction we will turn, 1 is clockwise, -1 is counter-clockwise
    // Returns:
    // heading = the new heading the gyro reports
    public void gyroTurn(int targetHeading, double maxSpeed, int direction) {

        int startHeading = gyroSensor.getHeading();
        int deceleration;
        int currentHeading;
        double oldSpeed;
        double speed = 0;
        double minSpeed = 0.05;
        double acceleration = 0.01;
        double ka = 0.01;             // Proportional acceleration constant
        lDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        // Calls turnCompeted function to determine if we need to keep turning
        while ((!turnCompleted(gyroSensor.getHeading(), targetHeading, 5, direction) && opModeIsActive())) {
            // Move speed management to separate function
            // Calculate the speed we should be moving at
            oldSpeed = speed;           // save our old speed for use later.
            currentHeading = gyroSensor.getHeading();
            // Reuses the degreesToTurn function by passing different values to obtain our error
            deceleration = degreesToTurn(targetHeading, currentHeading, direction);
            speed = deceleration * ka * direction;
            // Limit the acceleration of the motors speed at beginning of turns.
            if( Math.abs(speed) > Math.abs(oldSpeed))
                speed = oldSpeed + (direction * acceleration);
            // Set a minimum power for the motors to make sure they move
            if (Math.abs(speed) < minSpeed)
                speed = minSpeed * direction;
            // Don't exceed the maximium speed requested
            if (Math.abs(speed) > maxSpeed)
                speed = maxSpeed * direction;
            // Set the motor speeds
            lDrive1.setPower(speed);
            lDrive2.setPower(speed);
            rDrive1.setPower(-speed);
            rDrive2.setPower(-speed);
            telemetry.addData("Current Heading", gyroSensor.getHeading());
            telemetry.addData("Current Speed", speed);
            updateTelemetry(telemetry);
        }
        // Done with the turn so shut off motors
        lDrive1.setPower(0);
        lDrive2.setPower(0);
        rDrive1.setPower(0);
        rDrive2.setPower(0);
    }
    // Function used by the turnCompleted function (which is used by the gyroTurn function) to determine the degrees to turn.
    // Also used to determine error for proportional speed control in the gyroTurn function
    // by passing targetHeading and currentHeading instead of startHeading and targetHeading, respectively
    // Pass:
    // startHeading = heading that we are at before we turn
    // targetHeading = heading we want to turn to
    // direction = the direction we want to turn (1 for right, -1 for left)
    public static int degreesToTurn(int startHeading, int targetHeading, int direction) {
        int degreesToTurn;
        // Turning right
        if (direction == 1)
            degreesToTurn = targetHeading - startHeading;
            // degreesToTurn = targetHeading - currentHeading
            // Turning left
        else
            degreesToTurn = startHeading - targetHeading;
        if (degreesToTurn < 0) // Changed from "while"
            degreesToTurn = degreesToTurn + 360;
        else if (degreesToTurn > 360) // Changed from "while"
            degreesToTurn = degreesToTurn + 360;
        return (degreesToTurn);
    }
    // Function used by the GyroTurn function to determine if we have turned far enough.
    // Pulls from degreesToTurn function to determine degrees to turn.
    // Pass:
    // currentHeading = the heading the robot is currently at (live value, changes during the turn)
    // targetHeading = the heading we want the robot to be at once the turn is completed
    // range = the degrees of error we are allowing so that the robot doesn't spin in circles
    // if it overshoots by a small amount
    // direction = direction the robot is turning (1 for right, -1 for left)
    // Returns:
    // result = true if we have reached target heading, false if we haven't
    public static boolean turnCompleted(int currentHeading, int targetHeading, int range, int direction)  {
        // Value we want to stop at
        int stop;
        // Will be sent to the GyroTurn function; robot stops turning when true
        boolean result;
        // Turn right
        if (direction >= 1) {
            stop = targetHeading - range;
            if (stop >= 0){
                result = (currentHeading >= stop && currentHeading <= targetHeading);
            }
            else{
                result = ((currentHeading >= (stop+360) && currentHeading <=359)|| (currentHeading >= 0 && currentHeading <= targetHeading));
            }
        }
        // Turn left
        else {
            stop = targetHeading + range;
            if (stop <=359){
                result = (currentHeading <= stop && currentHeading >= targetHeading);
            }
            else{
                result = (currentHeading >= targetHeading && currentHeading <= 359) || (currentHeading >=0 && currentHeading <= (stop-360));
            }
        }
        return (result);
    }

    public void launch(double power, long time) throws InterruptedException{ //input time as seconds
        catapult.setPower(power);
        sleep(time*1000);
        catapult.setPower(0);
    }

    public void paddleMotor(double power, long time) throws InterruptedException{
        paddle.setPower(power);
        sleep(time);
        paddle.setPower(0);
    }

    public void feederPosition(int feederPos, long time) throws InterruptedException {
        feeder.setPosition(feederPos);
        sleep(time);
        feeder.setPosition(0);
        sleep(time);
    }

    public void runOpMode() throws InterruptedException {
        //##############Init##############
        feeder = hardwareMap.servo.get("f");
        catapult = hardwareMap.dcMotor.get("catapult");
        paddle = hardwareMap.dcMotor.get("paddle");
        lDrive1 = hardwareMap.dcMotor.get("lDrive1");
        lDrive2 = hardwareMap.dcMotor.get("lDrive2");
        rDrive1 = hardwareMap.dcMotor.get("rDrive1");
        rDrive2 = hardwareMap.dcMotor.get("rDrive2");
        lDrive2.setDirection(DcMotor.Direction.REVERSE);
        rDrive1.setDirection(DcMotor.Direction.REVERSE);
        setUpGyro();
        lDrive1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lDrive2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rDrive1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rDrive2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        waitForStart();

        // The code that runs the robot is here.
        launch(1, 1);
        paddleMotor(1, 2000);
        paddleMotor(0.3, 500);
        paddleMotor(1, 1600);
        feederPosition(45, 1000);
        launch(1, 1);
        moveMotors(-1, -1, 300);
        gyroTurn(320, .3, -1);
        moveMotors(1, 1, 500);
        gyroTurn(240, .3, -1);
        moveMotors(1, 1, 1000);
    }
}
