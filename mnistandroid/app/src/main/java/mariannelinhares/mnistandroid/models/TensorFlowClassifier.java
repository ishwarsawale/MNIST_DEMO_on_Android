package mariannelinhares.mnistandroid.models;

import android.content.res.AssetManager;
//Reads text from a character-input stream, buffering characters so as to provide for the efficient reading of characters, arrays, and lines.
import java.io.BufferedReader;
//for erros
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
//made by google, used as the window between android and tensorflow native C++
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


public class TensorFlowClassifier implements Classifier {

    private static final float THRESHOLD = 0.1f;

    private TensorFlowInferenceInterface tfHelper;

    private String name;
    private String inputName;
    private String outputName;
    private int inputSize;
    private boolean feedKeepProb;

    private List<String> labels;
    private float[] output;
    private String[] outputNames;

    private static List<String> readLabels(AssetManager am, String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(am.open(fileName)));

        String line;
        List<String> labels = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }

        br.close();
        return labels;
    }

    public static TensorFlowClassifier create(AssetManager assetManager, String name,
            String modelPath, String labelFile, int inputSize, String inputName, String outputName,
            boolean feedKeepProb) throws IOException {
        //intialize a classifier
        TensorFlowClassifier c = new TensorFlowClassifier();

        //store its name, input and output labels
        c.name = name;

        c.inputName = inputName;
        c.outputName = outputName;

        //read labels for label file
        c.labels = readLabels(assetManager, labelFile);

        //set its model path and where the raw asset files are
        c.tfHelper = new TensorFlowInferenceInterface(assetManager, modelPath);
        int numClasses = 10;

        //how big is the input?
        c.inputSize = inputSize;

        // Pre-allocate buffer.
        c.outputNames = new String[] { outputName };

        c.outputName = outputName;
        c.output = new float[numClasses];

        c.feedKeepProb = feedKeepProb;

        return c;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Classification recognize(final float[] pixels) {

        //using the interface
        //give it the input name, raw pixels from the drawing,
        //input size
        tfHelper.feed(inputName, pixels, 1, inputSize, inputSize, 1);

        //probabilities
        if (feedKeepProb) {
            tfHelper.feed("keep_prob", new float[] { 1 });
        }
        //get the possible outputs
        tfHelper.run(outputNames);

        //get the output
        tfHelper.fetch(outputName, output);

        // Find the best classification
        //for each output prediction
        //if its above the threshold for accuracy we predefined
        //write it out to the view
        Classification ans = new Classification();
        for (int i = 0; i < output.length; ++i) {
            System.out.println(output[i]);
            System.out.println(labels.get(i));
            if (output[i] > THRESHOLD && output[i] > ans.getConf()) {
                ans.update(output[i], labels.get(i));
            }
        }

        return ans;
    }
}
