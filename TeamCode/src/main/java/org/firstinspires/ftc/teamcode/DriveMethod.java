package org.firstinspires.ftc.teamcode;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
//import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.util.Range;

@Autonomous(name="Drive Method", group="Methods")
@Disabled
public class DriveMethod extends LinearOpMode {

    DcMotor lDrive1;
    DcMotor lDrive2;
    DcMotor rDrive1;
    DcMotor rDrive2;

    // This is the Drive Method
    // It will take in two static values: distance and maxSpeed.
    // It will then calculate the encoder counts to drive and drive the distance at the specified power,
    // accelerating to max speed for the first third of the distance, maintaining that speed for the second third,
    // and decelerating to a minimum speed for the last third.
    // If the robot deviates from the initial gyro heading, it will correct itself proportionally to the error.
    private void drive(double distance, double maxSpeed, int direction) throws InterruptedException {
        int ENCODER_CPR = 1120; // Encoder counts per Rev
        double gearRatio = 1.75; // [Gear Ratio]:1
        double circumference = 13.10; // Wheel circumference
        double ROTATIONS = distance / (circumference * gearRatio); // Number of rotations to drive
        double COUNTS = ENCODER_CPR * ROTATIONS; // Number of encoder counts to drive
        double speed = 0;
        double leftSpeed;
        double rightSpeed;

        rDrive1.setTargetPosition(rDrive1.getCurrentPosition() + (int) COUNTS);

        rDrive1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rDrive2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        lDrive1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        lDrive2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        sleep(500);

        if (direction == 1) {
            while (Math.abs(rDrive1.getCurrentPosition()) < Math.abs(rDrive1.getTargetPosition() - 5) && opModeIsActive()) {

                leftSpeed = maxSpeed;
                rightSpeed = maxSpeed;

                leftSpeed = Range.clip(leftSpeed, -1, 1);
                rightSpeed = Range.clip(rightSpeed, -1, 1);

                lDrive1.setPower(leftSpeed);
                rDrive1.setPower(rightSpeed);
                lDrive2.setPower(leftSpeed);
                rDrive2.setPower(rightSpeed);

                telemetry.addData("1. speed", speed);
                telemetry.addData("2. leftSpeed", leftSpeed);
                telemetry.addData("3. rightSpeed", rightSpeed);
                updateTelemetry(telemetry);
            }

            lDrive1.setPower(0);
            rDrive1.setPower(0);
            lDrive2.setPower(0);
            rDrive2.setPower(0);

            lDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            rDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            lDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            rDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        else if (direction == -1) {
            while (Math.abs(rDrive1.getCurrentPosition()) < Math.abs(rDrive1.getTargetPosition() - 5) && opModeIsActive()) {

                leftSpeed = maxSpeed;
                rightSpeed = maxSpeed;

                leftSpeed = Range.clip(leftSpeed, -1, 1);
                rightSpeed = Range.clip(rightSpeed, -1, 1);

                lDrive1.setPower(-leftSpeed);
                rDrive1.setPower(-rightSpeed);
                lDrive2.setPower(-leftSpeed);
                rDrive2.setPower(-rightSpeed);

                telemetry.addData("1. speed", speed);
                telemetry.addData("2. leftSpeed", leftSpeed);
                telemetry.addData("3. rightSpeed", rightSpeed);
                updateTelemetry(telemetry);
            }

            lDrive1.setPower(0);
            rDrive1.setPower(0);
            lDrive2.setPower(0);
            rDrive2.setPower(0);

            lDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            rDrive1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            lDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            rDrive2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        else {
            telemetry.addLine("Invalid direction");
            telemetry.update();
            sleep(10000);
        }
    }

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry.addData("Status", "Initialized");
        telemetry.update();
        double distance;
        double maxSpeed;
        int direction;

        lDrive1 = hardwareMap.dcMotor.get("lDrive1");
        lDrive2 = hardwareMap.dcMotor.get("lDrive2");
        rDrive1 = hardwareMap.dcMotor.get("rDrive1");
        rDrive2 = hardwareMap.dcMotor.get("rDrive2");
        lDrive1.setDirection(DcMotor.Direction.REVERSE);
        lDrive2.setDirection(DcMotor.Direction.REVERSE);

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

        distance = 90;
        maxSpeed = .4;
        direction = -1;
        drive(distance, maxSpeed, direction);

    }
}