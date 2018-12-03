package org.firstinspires.ftc.teamcode;

//import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;


//import org.firstinspires.ftc.robotcore.external.Telemetry;


public class AtlasClass extends LinearOpMode {

    DcMotor collectFlipperM, lb, rf, lf, rb, liftM, extendM, collectSpinnerM;
    double angle;
    Servo dumpS;
    private BNO055IMU imu;
    double speed;
    Orientation angles;
    double rightX;
    private ElapsedTime dumping = new ElapsedTime();




    AtlasClass(HardwareMap hardwareMap) throws InterruptedException {
        rb = hardwareMap.dcMotor.get("rbDriveM");
        rb.setPower(0);
        rb.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        lb = hardwareMap.dcMotor.get("lbDriveM");
        lb.setPower(0);
        lb.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        rf = hardwareMap.dcMotor.get("rfDriveM");
        rf.setPower(0);
        rf.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        lf = hardwareMap.dcMotor.get("lfDriveM");
        lf.setPower(0);
        lf.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        collectFlipperM = hardwareMap.dcMotor.get("collectFlipperM");
        collectFlipperM.setPower(0);
        collectFlipperM.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        liftM = hardwareMap.dcMotor.get("liftM");
        liftM.setPower(0);
        liftM.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        extendM = hardwareMap.dcMotor.get("extendM");
        extendM.setPower(0);
        extendM.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        collectSpinnerM = hardwareMap.dcMotor.get("collectSpinnerM");
        collectSpinnerM.setPower(0);
        collectSpinnerM.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        dumpS = hardwareMap.servo.get("deliveryS");
        dumpS.setPosition(0);

        BNO055IMU.Parameters parameters_IMU = new BNO055IMU.Parameters();
        parameters_IMU.angleUnit = BNO055IMU.AngleUnit.RADIANS;
        parameters_IMU.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters_IMU.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters_IMU.loggingEnabled = true;
        parameters_IMU.loggingTag = "IMU";
        parameters_IMU.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        imu = hardwareMap.get(BNO055IMU.class, "imu3");
        imu.initialize(parameters_IMU);

    }

    public void drive(){
        angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS);

        speed = Math.hypot(gamepad1.left_stick_x, gamepad1.left_stick_y);
        angle = Math.atan2(gamepad1.left_stick_y, gamepad1.left_stick_x) - Math.PI / 4;
        angle = angle + angles.firstAngle;
        rightX = gamepad1.right_stick_x;



        lf.setPower(-(speed * (Math.sin(angle)) + rightX));
        rf.setPower((speed * (Math.cos(angle))) - rightX);
        lb.setPower(-(speed * (Math.cos(angle)) + rightX));
        rb.setPower((speed * (Math.sin(angle))) - rightX);

        telemetry.addData("lb pwoer", lb.getPower());
        telemetry.addData("rb pwoer", rb.getPower());
        telemetry.addData("rf pwoer", rf.getPower());
        telemetry.addData("lf pwoer", lf.getPower());
        telemetry.update();
    }

    public void dumpAndLift(){
        if(gamepad2.right_bumper){
            dumpS.setPosition(1);
            dumping.reset();
        }
        else {
            dumpS.setPosition(0);
        }

        if(dumping.milliseconds() < 100){
            liftM.setPower(0.3);
        }
        else {
            liftM.setPower(-gamepad2.right_stick_y);

        }
    }

    public void collectionSpin(){
        if     (gamepad2.dpad_up)   collectSpinnerM.setPower(0.6);
        else if(gamepad2.dpad_down) collectSpinnerM.setPower(-0.6);
        else if(gamepad2.dpad_left) collectSpinnerM.setPower(0);
    }

    public void collectionFlip() {
        collectFlipperM.setPower(gamepad2.right_trigger - gamepad2.left_trigger);
    }

    public void extend(){
        extendM.setPower(gamepad2.left_stick_y);
    }

    //never actually run this, just threw it in so that we can call this a linear opMode for telemetry data and controller inputs
    @Override
    public void runOpMode() {

    }
}