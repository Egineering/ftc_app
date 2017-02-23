package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
//import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchImpl;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

@Autonomous(name="cf_2_beacon_red_decatur", group="LinearOpMode")
@Disabled

public class cf_2_beacon_red_decatur extends LinearOpMode {

    private DcMotor catapult;
    //private DcMotor sweeper;
    private DcMotor lDrive1;
    private DcMotor lDrive2;
    private DcMotor rDrive1;
    private DcMotor rDrive2;
    private Servo button;
    private Servo hopper;
    private Servo belt;

    private TouchSensor touch;
    private GyroSensor gyroSensor;
    private ModernRoboticsI2cGyro gyro;
    private ColorSensor color;
    private OpticalDistanceSensor fODSensor;
    private OpticalDistanceSensor bODSensor;

    I2cAddr RANGE1ADDRESS = new I2cAddr(0x14); //Default I2C address for MR Range (7-bit)
    I2cAddr RANGE2ADDRESS = new I2cAddr(0x18);


    //private I2cAddr RANGE1ADDRESS = new I2cAddr(0x28); //Default I2C address for MR Range (7-bit)
    private static final int RANGE1_REG_START = 0x04; //Register to start reading
    private static final int RANGE1_READ_LENGTH = 2; //Number of byte to read
    private ModernRoboticsI2cRangeSensor rangeSensor;
    //2nd range
    private static final int RANGE2_REG_START = 0x04; //Register to start reading
    private static final int RANGE2_READ_LENGTH = 2; //Number of byte to read
    private ModernRoboticsI2cRangeSensor rangeSensor2;

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
        gyro = (ModernRoboticsI2cGyro) gyroSensor;
        // calibrate the gyro.
        gyroSensor.calibrate();
        while (gyroSensor.isCalibrating())  {
            sleep(50);
            telemetry.addLine("Calibrating Gyro");
            updateTelemetry(telemetry);
        }
        // End of setting up Gyro
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
    public void gyroTurn(int targetHeading, double maxSpeed, int direction) throws InterruptedException {
        //int startHeading = gyroSensor.getHeading();
        int deceleration;
        int currentHeading;
        double oldSpeed;
        double speed = 0;
        double minSpeed = 0.2;
        double acceleration = 0.01;
        double ka = 0.01;             // Proportional acceleration constant

        lDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        idle();

        // Calls turnCompeted function to determine if we need to keep turning
        while ((!turnCompleted(gyroSensor.getHeading(), targetHeading, 3, direction) && opModeIsActive())) {

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

    public void driveForwardToWall() throws InterruptedException {
        I2cDevice RANGE1 = hardwareMap.i2cDevice.get("range");
        I2cDeviceSynch RANGE1Reader = new I2cDeviceSynchImpl(RANGE1, RANGE1ADDRESS, false);
        RANGE1Reader.engage();
        byte[] range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        RANGE1Reader.engage();
        //prepare second range sensor
        I2cDevice RANGE2 = hardwareMap.i2cDevice.get("range2");
        I2cDeviceSynch RANGE2Reader = new I2cDeviceSynchImpl(RANGE2, RANGE2ADDRESS, false);
        byte[] range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        RANGE2Reader.engage();
        telemetry.addData("Status", "Initialized");
        range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
        telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
        telemetry.update();
        lDrive1.setPower(0.4);
        rDrive1.setPower(0.4);
        lDrive2.setPower(0.4);
        rDrive2.setPower(0.4);
        sleep(2000);
        while (range1Cache[0] >= 17 && opModeIsActive()) {
            range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
            range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
            telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
            telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
            if (range1Cache[0] <= 200) {
                sleep(1);
            }
        }
        lDrive1.setPower(0);
        rDrive1.setPower(0);
        lDrive2.setPower(0);
        rDrive2.setPower(0);
    }

    public void driveBackwardToWall() throws InterruptedException {
        lDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        I2cDevice RANGE1 = hardwareMap.i2cDevice.get("range");
        I2cDeviceSynch RANGE1Reader = new I2cDeviceSynchImpl(RANGE1, RANGE1ADDRESS, false);
        RANGE1Reader.engage();
        byte[] range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        RANGE1Reader.engage();
        //prepare second range sensor
        I2cDevice RANGE2 = hardwareMap.i2cDevice.get("range2");
        I2cDeviceSynch RANGE2Reader = new I2cDeviceSynchImpl(RANGE2, RANGE2ADDRESS, false);
        byte[] range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        RANGE2Reader.engage();
        telemetry.addData("Status", "Initialized");
        range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
        telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
        telemetry.update();
        lDrive1.setPower(-0.55);
        rDrive1.setPower(-0.4);
        lDrive2.setPower(-0.55);
        rDrive2.setPower(-0.4);
        sleep(2000);
        while (range2Cache[0] >= 17 && opModeIsActive()) {
            range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
            range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
            telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
            telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
            if (range2Cache[0] <= 200) {
                sleep(1);
            }
        }
        lDrive1.setPower(0);
        rDrive1.setPower(0);
        lDrive2.setPower(0);
        rDrive2.setPower(0);
    }

    public void wallTrack() throws InterruptedException {
        //I2cDevice RANGE1 = hardwareMap.i2cDevice.get("range");
        //I2cDeviceSynch RANGE1Reader = new I2cDeviceSynchImpl(RANGE1, RANGE1ADDRESS, false);
        //RANGE1Reader.engage();
        //byte[] range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        //RANGE1Reader.engage();
        //prepare second range sensor
        I2cDevice RANGE2 = hardwareMap.i2cDevice.get("range2");
        I2cDeviceSynch RANGE2Reader = new I2cDeviceSynchImpl(RANGE2, RANGE2ADDRESS, false);
        byte[] range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        RANGE2Reader.engage();
        telemetry.addData("Status", "Initialized");
        //range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        //telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
        telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
        telemetry.update();

        while (bODSensor.getRawLightDetected() < .075 && opModeIsActive()) {
            //range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
            range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
            double range2 = range2Cache[0];

            //telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
            telemetry.addData("Range2 value:", (range2));
            telemetry.addData("bOD light", (bODSensor.getRawLightDetected()));
            double error;
            if (range2 > 200)
                error = 0;
            else if (range2 < 0)
                error = 0;
            else
                error = ((range2-12)/60);

            error = Range.clip(error, -.3, .3);
            telemetry.addData("Error", error);

            double leftSpeed = -.25-error;
            double rightSpeed = -.25+error;
            leftSpeed = Range.clip(leftSpeed, -1, 1);
            rightSpeed = Range.clip(rightSpeed, -1, 1);

            lDrive1.setPower(leftSpeed);
            lDrive2.setPower(leftSpeed);
            rDrive1.setPower(rightSpeed);
            rDrive2.setPower(rightSpeed);

//            if (range2Cache[0] < 15) {
//                // Adjust right
//                leftSpeed = -.3-error;
//                rightSpeed = -.3+error;
//                leftSpeed = Range.clip(leftSpeed, -1, 1);
//                rightSpeed = Range.clip(rightSpeed, -1, 1);
//                lDrive1.setPower(leftSpeed);
//                lDrive2.setPower(leftSpeed);
//                rDrive1.setPower(rightSpeed);
//                rDrive2.setPower(rightSpeed);
//                telemetry.addLine("Adjusting Right");
//            }
//            else if (range2Cache[0] > 15){
//                // Adjust left
//                leftSpeed = -.3+error;
//                rightSpeed = -.3-error;
//                leftSpeed = Range.clip(leftSpeed, -1, 1);
//                rightSpeed = Range.clip(rightSpeed, -1, 1);
//                lDrive1.setPower(leftSpeed);
//                lDrive2.setPower(leftSpeed);
//                rDrive1.setPower(rightSpeed);
//                rDrive2.setPower(rightSpeed);
//                telemetry.addLine("Adjusting Left");
//            }
//            else {
//                // Drive straight backward
//                leftSpeed = -.3;
//                rightSpeed = -.3;
//                lDrive1.setPower(leftSpeed);
//                lDrive2.setPower(leftSpeed);
//                rDrive1.setPower(rightSpeed);
//                rDrive2.setPower(rightSpeed);
//                telemetry.addLine("Driving Straight");
//            }
            telemetry.addData("leftSpeed",(leftSpeed));
            telemetry.addData("rightSpeed",(rightSpeed));
            telemetry.update();
        }
        lDrive1.setPower(0);
        lDrive2.setPower(0);
        rDrive1.setPower(0);
        rDrive2.setPower(0);
    }

    public void wallTrackBack() throws InterruptedException {
        I2cDevice RANGE1 = hardwareMap.i2cDevice.get("range");
        I2cDeviceSynch RANGE1Reader = new I2cDeviceSynchImpl(RANGE1, RANGE1ADDRESS, false);
        RANGE1Reader.engage();
        byte[] range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        RANGE1Reader.engage();
        //prepare second range sensor
        I2cDevice RANGE2 = hardwareMap.i2cDevice.get("range2");
        I2cDeviceSynch RANGE2Reader = new I2cDeviceSynchImpl(RANGE2, RANGE2ADDRESS, false);
        byte[] range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        RANGE2Reader.engage();
        telemetry.addData("Status", "Initialized");
        //range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        //telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
        telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
        telemetry.update();
        while (fODSensor.getRawLightDetected() < .1 && opModeIsActive()) {
            range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
            range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
            double range2 = range2Cache[0];
            double range1 = range1Cache[0];
            //telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
            telemetry.addData("Range2 value:", (range2));
            telemetry.addData("bOD light", (bODSensor.getRawLightDetected()));

            double error;
            if (range1 > 200)
                error = 0;
            else if (range1 < 0)
                error = 0;
            else
                error = ((range1-12)/60);

            error = Range.clip(error, -.3, .3);
            telemetry.addData("Error", error);

            double leftSpeed = .25+error;
            double rightSpeed = .25-error;



            lDrive1.setPower(leftSpeed);
            lDrive2.setPower(leftSpeed);
            rDrive1.setPower(rightSpeed);
            rDrive2.setPower(rightSpeed);
//            if (range2Cache[0] < 15) {
//                // Adjust right
//                leftSpeed = -.3-error;
//                rightSpeed = -.3+error;
//                leftSpeed = Range.clip(leftSpeed, -1, 1);
//                rightSpeed = Range.clip(rightSpeed, -1, 1);
//                lDrive1.setPower(leftSpeed);
//                lDrive2.setPower(leftSpeed);
//                rDrive1.setPower(rightSpeed);
//                rDrive2.setPower(rightSpeed);
//                telemetry.addLine("Adjusting Right");
//            }
//            else if (range2Cache[0] > 15){
//                // Adjust left
//                leftSpeed = -.3+error;
//                rightSpeed = -.3-error;
//                leftSpeed = Range.clip(leftSpeed, -1, 1);
//                rightSpeed = Range.clip(rightSpeed, -1, 1);
//                lDrive1.setPower(leftSpeed);
//                lDrive2.setPower(leftSpeed);
//                rDrive1.setPower(rightSpeed);
//                rDrive2.setPower(rightSpeed);
//                telemetry.addLine("Adjusting Left");
//            }
//            else {
//                // Drive straight backward
//                leftSpeed = -.3;
//                rightSpeed = -.3;
//                lDrive1.setPower(leftSpeed);
//                lDrive2.setPower(leftSpeed);
//                rDrive1.setPower(rightSpeed);
//                rDrive2.setPower(rightSpeed);
//                telemetry.addLine("Driving Straight");
//            }
            telemetry.addData("leftSpeed",(leftSpeed));
            telemetry.addData("rightSpeed",(rightSpeed));
            telemetry.update();
        }
        lDrive1.setPower(0);
        lDrive2.setPower(0);
        rDrive1.setPower(0);
        rDrive2.setPower(0);
    }


    public void lineUp() throws InterruptedException {
        I2cDevice RANGE1 = hardwareMap.i2cDevice.get("range");
        I2cDeviceSynch RANGE1Reader = new I2cDeviceSynchImpl(RANGE1, RANGE1ADDRESS, false);
        RANGE1Reader.engage();
        byte[] range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        RANGE1Reader.engage();
        //prepare second range sensor
        I2cDevice RANGE2 = hardwareMap.i2cDevice.get("range2");
        I2cDeviceSynch RANGE2Reader = new I2cDeviceSynchImpl(RANGE2, RANGE2ADDRESS, false);
        byte[] range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        RANGE2Reader.engage();
        telemetry.addData("Status", "Initialized");
        range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
        range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
        telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
        telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
        telemetry.update();
        telemetry.update();
        if (range1Cache[0] > range2Cache[0]) {
            lDrive1.setPower(0.2);
            rDrive1.setPower(-0.2);
            lDrive2.setPower(0.2);
            rDrive2.setPower(-0.2);
            while (range1Cache[0] > range2Cache[0]  && opModeIsActive()) {
                range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
                range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
                telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
                telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
                if (range1Cache[0] < 200) {
                    lDrive1.setPower(0);
                    rDrive1.setPower(0);
                    lDrive2.setPower(0);
                    rDrive2.setPower(0);
                    sleep(10);
                    lDrive1.setPower(0.2);
                    rDrive1.setPower(-0.2);
                    lDrive2.setPower(0.2);
                    rDrive2.setPower(-0.2);
                }
                else if (range2Cache[0] < 200) {
                    lDrive1.setPower(0);
                    rDrive1.setPower(0);
                    lDrive2.setPower(0);
                    rDrive2.setPower(0);
                    lDrive1.setPower(0.2);
                    rDrive1.setPower(-0.2);
                    lDrive2.setPower(0.2);
                    rDrive2.setPower(-0.2);
                }
            }
            lDrive1.setPower(0);
            rDrive1.setPower(0);
            lDrive2.setPower(0);
            rDrive2.setPower(0);
        }
        else if (range1Cache[0] < range2Cache[0]) {
            lDrive1.setPower(-0.2);
            rDrive1.setPower(0.2);
            lDrive2.setPower(-0.2);
            rDrive2.setPower(0.2);
            while (range1Cache[0] < range2Cache[0]&& opModeIsActive()) {
                range1Cache = RANGE1Reader.read(RANGE1_REG_START, RANGE1_READ_LENGTH);
                range2Cache = RANGE2Reader.read(RANGE2_REG_START, RANGE2_READ_LENGTH);
                telemetry.addData("Range value:", (range1Cache[0] & 0xFF));
                telemetry.addData("Range2 value:", (range2Cache[0] & 0xFF));
                if (range1Cache[0] < 200) {
                    lDrive1.setPower(0);
                    rDrive1.setPower(0);
                    lDrive2.setPower(0);
                    rDrive2.setPower(0);
                    sleep(100);
                    lDrive1.setPower(-0.2);
                    rDrive1.setPower(0.2);
                    lDrive2.setPower(-0.2);
                    rDrive2.setPower(0.2);
                }
                else if (range2Cache[0] < 200) {
                    lDrive1.setPower(0);
                    rDrive1.setPower(0);
                    lDrive2.setPower(0);
                    rDrive2.setPower(0);
                    sleep(100);
                    lDrive1.setPower(-0.2);
                    rDrive1.setPower(0.2);
                    lDrive2.setPower(-0.2);
                    rDrive2.setPower(0.2);
                }
            }
            lDrive1.setPower(0);
            rDrive1.setPower(0);
            lDrive2.setPower(0);
            rDrive2.setPower(0);
        }
        else {
            lDrive1.setPower(0);
            rDrive1.setPower(0);
            lDrive2.setPower(0);
            rDrive2.setPower(0);
        }
    }

    // Fully working as of 2/8/17
    private void recognizeColorRed(int direction) throws InterruptedException {
        color.enableLed(false);
        while (color.red() < (color.blue())+1) {
            rDrive1.setPower(.2*direction);
            rDrive2.setPower(.2*direction);
            lDrive1.setPower(.3*direction);
            lDrive2.setPower(.3*direction);
            telemetry.addData("Blue", color.blue());
            telemetry.addData("Red", color.red());
            telemetry.update();
        }
        rDrive1.setPower(0);
        rDrive2.setPower(0);
        lDrive1.setPower(0);
        lDrive2.setPower(0);
        button.setPosition(0);
        sleep(2000);
        button.setPosition(1);
        sleep(2000);
        button.setPosition(0.5);
        telemetry.addData("Blue", color.blue());
        telemetry.addData("Red", color.red());
        telemetry.update();
    }

    // Fully working as of 2/8/17
    private void recognizeColorBlue(int direction) throws InterruptedException {
        color.enableLed(false);
        while (color.blue() < (color.red()+1)) {
            rDrive1.setPower(.2*direction);
            rDrive2.setPower(.2*direction);
            lDrive1.setPower(.2*direction);
            lDrive2.setPower(.2*direction);
            telemetry.addData("Blue", color.blue());
            telemetry.addData("Red", color.red());
            telemetry.update();
        }
        rDrive1.setPower(0);
        rDrive2.setPower(0);
        lDrive1.setPower(0);
        lDrive2.setPower(0);
        button.setPosition(0);
        sleep(2000);
        button.setPosition(1);
        sleep(2000);
        button.setPosition(0.5);
        telemetry.addData("Blue", color.blue());
        telemetry.addData("Red", color.red());
        telemetry.update();
    }

    public void runOpMode() throws InterruptedException {
        //##############Init##############
        rDrive1 = hardwareMap.dcMotor.get("rDrive1");
        rDrive2 = hardwareMap.dcMotor.get("rDrive2");
        lDrive1 = hardwareMap.dcMotor.get("lDrive1");
        lDrive2 = hardwareMap.dcMotor.get("lDrive2");
        lDrive1.setDirection(DcMotor.Direction.REVERSE);
        lDrive2.setDirection(DcMotor.Direction.REVERSE);

        //sweeper = hardwareMap.dcMotor.get("sweeper");
        catapult = hardwareMap.dcMotor.get("catapult");
        button = hardwareMap.servo.get("button");
        hopper = hardwareMap.servo.get("hopper");
        belt = hardwareMap.servo.get("belt");
        touch = hardwareMap.touchSensor.get("t");
        color = hardwareMap.colorSensor.get("color");


        fODSensor = hardwareMap.opticalDistanceSensor.get("fOD");
        bODSensor = hardwareMap.opticalDistanceSensor.get("bOD");
        hopper.setPosition(0.8);
        button.setPosition(0.5);
        belt.setPosition(.5);

        double distance;
        double maxSpeed;
        int targetHeading;
        int direction;
        long time;
        double parallel;

        lDrive1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lDrive2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rDrive1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rDrive2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        idle();
        lDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        idle();

        waitForStart();

        // Drive backward until the range sensor detects the wall
        driveBackwardToWall();
        // Turn until parallel with the wall using both range sensors
        lineUp();
        // Track backward along the wall until the white line
        wallTrack();
        // Push the button for red
        sleep(500);
        direction = -1;
        recognizeColorRed(direction);
        // Drive forward past the line
        rDrive1.setPower(0.4);
        rDrive2.setPower(0.4);
        lDrive1.setPower(0.4);
        lDrive2.setPower(0.4);
        sleep(1000);
        rDrive1.setPower(0);
        rDrive2.setPower(0);
        lDrive1.setPower(0);
        lDrive2.setPower(0);
        // Track forward along the wall until the next white line
        wallTrackBack();
        sleep(1000);
        // Push the button for red
        recognizeColorRed(1);
        // Initialize and calibrate gyro
        /*setUpGyro();
        // Turn 90 degrees left
        targetHeading = 270;
        maxSpeed = .3;
        direction = -1;
        gyroTurn(targetHeading, maxSpeed, direction);*/

    }
}
