package mariannelinhares.mnistandroid


import android.app.Activity
import android.graphics.PointF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.ArrayList
import mariannelinhares.mnistandroid.models.Classifier
import mariannelinhares.mnistandroid.models.TensorFlowClassifier
import mariannelinhares.mnistandroid.views.DrawModel
import mariannelinhares.mnistandroid.views.DrawView// basic list

class MainActivity : Activity(), View.OnClickListener, View.OnTouchListener {

    // ui elements
    private var clearBtn: Button? = null
    private var classBtn: Button? = null
    private var resText: TextView? = null
    private val mClassifiers = ArrayList<Classifier>()

    // views
    private var drawModel: DrawModel? = null
    private var drawView: DrawView? = null
    private val mTmpPiont = PointF()

    private var mLastX: Float = 0.toFloat()
    private var mLastY: Float = 0.toFloat()

    override // In the onCreate() method, you perform basic application startup logic that should happen
            //only once for the entire life of the activity.
    fun onCreate(savedInstanceState: Bundle?) {
        //initialization
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //get drawing view from XML (where the finger writes the number)
        drawView = findViewById(R.id.draw) as DrawView
        //get the model object
        drawModel = DrawModel(PIXEL_WIDTH, PIXEL_WIDTH)

        //init the view with the model object
        drawView!!.setModel(drawModel)
        // give it a touch listener to activate when the user taps
        drawView!!.setOnTouchListener(this)

        //clear button
        //clear the drawing when the user taps
        clearBtn = findViewById(R.id.btn_clear) as Button
        clearBtn!!.setOnClickListener(this)

        //class button
        //when tapped, this performs classification on the drawn image
        classBtn = findViewById(R.id.btn_class) as Button
        classBtn!!.setOnClickListener(this)

        // res text
        //this is the text that shows the output of the classification
        resText = findViewById(R.id.tfRes) as TextView

        // tensorflow
        //load up our saved model to perform inference from local storage
        loadModel()
    }

    //the activity lifecycle

    override //OnResume() is called when the user resumes his Activity which he left a while ago,
            // //say he presses home button and then comes back to app, onResume() is called.
    fun onResume() {
        drawView!!.onResume()
        super.onResume()
    }

    override //OnPause() is called when the user receives an event like a call or a text message,
            // //when onPause() is called the Activity may be partially or completely hidden.
    fun onPause() {
        drawView!!.onPause()
        super.onPause()
    }

    //creates a model object in memory using the saved tensorflow protobuf model file
    //which contains all the learned weights
    private fun loadModel() {
        //The Runnable interface is another way in which you can implement multi-threading other than extending the
        // //Thread class due to the fact that Java allows you to extend only one class. Runnable is just an interface,
        // //which provides the method run.
        // //Threads are implementations and use Runnable to call the method run().
        Thread(Runnable {
            try {
                //add 2 classifiers to our classifier arraylist
                //the tensorflow classifier and the keras classifier
                mClassifiers.add(TensorFlowClassifier.create(assets, "TensorFlow", "opt_mnist_convnet-tf.pb", "labels.txt", PIXEL_WIDTH, "input", "output", true))
                mClassifiers.add(TensorFlowClassifier.create(assets, "Keras", "opt_mnist_convnet-keras.pb", "labels.txt", PIXEL_WIDTH, "conv2d_1_input", "dense_2/Softmax", false))
            } catch (e: Exception) {
                //if they aren't found, throw an error!
                throw RuntimeException("Error initializing classifiers!", e)
            }
        }).start()
    }

    override fun onClick(view: View) {
        //when the user clicks something
        if (view.id == R.id.btn_clear) {
            //if its the clear button
            //clear the drawing
            drawModel!!.clear()
            drawView!!.reset()
            drawView!!.invalidate()
            //empty the text view
            resText!!.text = ""
        } else if (view.id == R.id.btn_class) {
            //if the user clicks the classify button
            //get the pixel data and store it in an array
            val pixels = drawView!!.pixelData

            //init an empty string to fill with the classification output
            var text = ""
            //for each classifier in our array
            for (classifier in mClassifiers) {
                //perform classification on the image
                val res = classifier.recognize(pixels)
                //if it can't classify, output a question mark
                if (res.label == null) {
                    text += classifier.name() + ": ?\n"
                } else {
                    //else output its name
                    text += String.format("%s: %s, %f\n", classifier.name(), res.label, res.conf)
                }
            }
            resText!!.text = text
        }
    }

    override //this method detects which direction a user is moving
            //their finger and draws a line accordingly in that
            //direction
    fun onTouch(v: View, event: MotionEvent): Boolean {
        //get the action and store it as an int
        val action = event.action and MotionEvent.ACTION_MASK
        //actions have predefined ints, lets match
        //to detect, if the user has touched, which direction the users finger is
        //moving, and if they've stopped moving

        //if touched
        if (action == MotionEvent.ACTION_DOWN) {
            //begin drawing line
            processTouchDown(event)
            return true
            //draw line in every direction the user moves
        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event)
            return true
            //if finger is lifted, stop drawing
        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp()
            return true
        }
        return false
    }

    //draw line down

    private fun processTouchDown(event: MotionEvent) {
        //calculate the x, y coordinates where the user has touched
        mLastX = event.x
        mLastY = event.y
        //user them to calcualte the position
        drawView!!.calcPos(mLastX, mLastY, mTmpPiont)
        //store them in memory to draw a line between the
        //difference in positions
        val lastConvX = mTmpPiont.x
        val lastConvY = mTmpPiont.y
        //and begin the line drawing
        drawModel!!.startLine(lastConvX, lastConvY)
    }

    //the main drawing function
    //it actually stores all the drawing positions
    //into the drawmodel object
    //we actually render the drawing from that object
    //in the drawrenderer class
    private fun processTouchMove(event: MotionEvent) {
        val x = event.x
        val y = event.y

        drawView!!.calcPos(x, y, mTmpPiont)
        val newConvX = mTmpPiont.x
        val newConvY = mTmpPiont.y
        drawModel!!.addLineElem(newConvX, newConvY)

        mLastX = x
        mLastY = y
        drawView!!.invalidate()
    }

    private fun processTouchUp() {
        drawModel!!.endLine()
    }

    companion object {

        private val PIXEL_WIDTH = 28
    }
}