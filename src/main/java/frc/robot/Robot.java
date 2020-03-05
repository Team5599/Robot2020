/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

//Import(s) For Standard Robot Use
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

//Import(s) For TalonFX
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;

//Import(s) For Limelight
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

//Import(s) For Color Sensor
import com.revrobotics.ColorSensorV3;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.SpeedControllerGroup;

//Import(s) For SparkMAX
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.CANSparkMax;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the TimedRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  // Declaration Of Objects
  // Talon FX (Falcon 500) Motor(s)
  WPI_TalonFX m_leftOne, m_leftTwo, m_rightOne, m_rightTwo;

  // Controller(s)
  XBoxController controller;

  // 

  // For Limelight
  NetworkTable table;
  NetworkTableEntry tx, ty, ta;
  double xOffset, yOffset, targetArea;

  // For Color Sensor V3
  I2C.Port p_color;
  ColorSensorV3 s_color;

  // For Retrieving FMS Data
  String gameData;

  // CAN SparkMax Motor(s)
  CANSparkMax controlPanelSpinner;
  CANSparkMax shooter;

  SpeedControllerGroup leftDrive;
  SpeedControllerGroup rightDrive;

  DifferentialDrive driveTrain;

  /**
   * This function is run when the robot is first started up and should be used
   * for any initialization code.
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);

    // Initializing Objects
    // Left Talon Motor(s)
    m_leftOne = new WPI_TalonFX(0);
    m_leftTwo = new WPI_TalonFX(1);

    // Right Talon Motor(s)
    m_rightOne = new WPI_TalonFX(3);
    m_rightTwo = new WPI_TalonFX(4);

    // Controller(s)
    controller = new XBoxController(0);

    // For Limelight
    table = NetworkTableInstance.getDefault().getTable("limelight");
    tx = table.getEntry("tx");
    ty = table.getEntry("ty");
    ta = table.getEntry("ta");

    // For Color Sensor V3
    p_color = I2C.Port.kOnboard;
    s_color = new ColorSensorV3(p_color);

    // Control Panel Spinner Motor
    controlPanelSpinner = new CANSparkMax(0, MotorType.kBrushless);

    // Shooter Motor
    shooter = new CANSparkMax(1, MotorType.kBrushless);

    leftDrive = new SpeedControllerGroup(m_leftOne, m_leftTwo);
    rightDrive = new SpeedControllerGroup(m_rightOne, m_rightTwo);

    driveTrain = new DifferentialDrive(leftDrive, rightDrive);
  }

  /**
   * This function is called every robot packet, no matter the mode. Use this for
   * items like diagnostics that you want ran during disabled, autonomous,
   * teleoperated and test.
   *
   * <p>
   * This runs after the mode specific periodic functions, but before LiveWindow
   * and SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {

  }

  /**
   * This autonomous (along with the chooser code above) shows how to select
   * between different autonomous modes using the dashboard. The sendable chooser
   * code works with the Java SmartDashboard. If you prefer the LabVIEW Dashboard,
   * remove all of the chooser code and uncomment the getString line to get the
   * auto name from the text box below the Gyro
   *
   * <p>
   * You can add additional auto modes by adding additional comparisons to the
   * switch structure below with additional strings. If using the SendableChooser
   * make sure to add them to the chooser code above as well.
   */
  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);
  }

  /**
   * This function is called periodically during autonomous.
   */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
    case kCustomAuto:
      // Put custom auto code here
      break;
    case kDefaultAuto:
    default:
      // Put default auto code here
      break;
    }
  }

  /**
   * This function is called periodically during operator control.
   */
  @Override
  public void teleopPeriodic() {

    /*
     * If the 'X' button is pressed then, if the vision target is to the left,
     * rotate left. If the vision target is to the right, rotate right.
     */
    if (controller.getXButton()) {
      xOffset = tx.getDouble(0.0);
      yOffset = ty.getDouble(0.0);
      targetArea = ta.getDouble(0.0);

      if (xOffset < -1) {
        driveTrain.tankDrive(-0.5, 0.5);
      } else if (xOffset > 9) {
        driveTrain.tankDrive(0.5, -0.5);
      }
    } else {
      driveTrain.tankDrive(controller.getLeftThumbstickY(), controller.getRightThumbstickY());
    }

    /*
     * If the 'A' button is pressed, then, spin the motor. If the current color
     * detected is equal to FMS, stop the motor.
     */
    if (controller.getAButton()) {
      gameData = DriverStation.getInstance().getGameSpecificMessage();

      controlPanelSpinner.set(0.5);

      if (gameData.length() > 0) {
        if ((s_color.getRed() > s_color.getGreen()) & gameData.charAt(0) == 'R') {
          SmartDashboard.putString("Color", "Red?");
          controlPanelSpinner.set(0);
        } else if ((s_color.getBlue() > s_color.getGreen()) & gameData.charAt(0) == 'B') {
          SmartDashboard.putString("Color", "Blue?");
          controlPanelSpinner.set(0);
        } else if (((s_color.getRed() > s_color.getBlue()) & (s_color.getGreen() > s_color.getBlue()))
            & gameData.charAt(0) == 'Y') {
          SmartDashboard.putString("Color", "Yellow?");
          controlPanelSpinner.set(0);
        } else if (((s_color.getGreen() > s_color.getRed()) & (s_color.getGreen() > s_color.getBlue()))
            & gameData.charAt(0) == 'G') {
          SmartDashboard.putString("Color", "Green?");
          controlPanelSpinner.set(0);
        }
      }
    } else {
      controlPanelSpinner.set(0);
    }

    /*
     * If '' button is pressed, motor for shooter is spun forward to shoot out battery cell (balls).
     * If '' button is pressed, motor for shooter is spun backwards to intake battery cell.
     * When button is released, motor stops.
     */
     //NOTE: This feature is still being worked on, so conditions and set values may change
     //if(*condition goes here*) shooter.set(0.7);
     //else if(*condition goes here*) shooter.set(-0.7);
     //else shooter.set(0);

  }

  /**
   * This function is called periodically during test mode.
   */
  @Override
  public void testPeriodic() {
  }
}
