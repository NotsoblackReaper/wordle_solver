package at.demski.wordle_solver;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Solver {
    public double static_scores_weight = 0.1;
    public double dynamic_scores_weight = 0.3;
    public double wholeWord_scores_weight = 0.6;

    List<String> blacklist = new ArrayList<>();
    double[] letterScores = {1.0 / 4,   //A
            1.0 / 18,  //B
            1.0 / 10,  //C
            1.0 / 11,  //D
            1.0 / 1,   //E
            1.0 / 20,  //F
            1.0 / 13,  //G
            1.0 / 17,  //H
            1.0 / 3,   //I
            1.0 / 26,  //J
            1.0 / 16,  //K
            1.0 / 9,   //L
            1.0 / 15,  //M
            1.0 / 6,   //N
            1.0 / 8,   //0
            1.0 / 14,  //P
            1.0 / 25,  //Q
            1.0 / 5,   //R
            1.0 / 2,   //S
            1.0 / 4,   //T
            1.0 / 12,  //U
            1.0 / 21,  //V
            1.0 / 22,  //W
            1.0 / 24,  //X
            1.0 / 19,  //Y
            1.0 / 23    //Z
    };
    List<Map<Character, Double>> dynamicLetterScores;
    Map<Character, Double> dynamicLetterScoresWholeWord;

    public Solver() {
        blacklist.add("SATIE");
        blacklist.add("TAINO");
    }

    public List<String> readWordsOfLength(String filename, int length) throws IOException {
        ArrayList<String> words = new ArrayList<>();
        File file = new File(filename);
        try (FileInputStream iStream = new FileInputStream(file); Scanner sc = new Scanner(iStream)) {
            while (sc.hasNextLine()) {
                String word = sc.nextLine();
                if (word.length() == length && !((int) word.charAt(0) < (int) 'A'))
                    words.add(word.toUpperCase());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return words;
    }

    public List<Map.Entry<Character, Integer>> countLettersAllPositions(List<String> words, int length) {
        Map<Character, Integer> temp_res = new HashMap<>();
        for (String word : words) {
            for (int i = 0; i < length; ++i) {
                if (word.charAt(i) >= 'A' && word.charAt(i) <= 'Z')
                    if (temp_res.containsKey(word.charAt(i)))
                        temp_res.replace(word.charAt(i), temp_res.get(word.charAt(i)) + 1);
                    else
                        temp_res.put(word.charAt(i), 1);
            }
        }
        return temp_res.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
    }

    public Map<Character, Double> convertToScoresAllLetters(List<Map.Entry<Character, Integer>> letters) {
        Map<Character, Double> result = new HashMap<>();
        for (int j = 0; j < letters.size(); ++j) {
            result.put(letters.get(j).getKey(), 1 / (26.0 - j));
        }
        return result;
    }

    public List<List<Map.Entry<Character, Integer>>> countLetters(List<String> words, int length) {
        List<Map<Character, Integer>> temp_res = new ArrayList<>();
        for (int i = 0; i < length; ++i)
            temp_res.add(new HashMap<>());
        for (String word : words) {
            for (int i = 0; i < length; ++i) {
                if (word.charAt(i) >= 'A' && word.charAt(i) <= 'Z')
                    if (temp_res.get(i).containsKey(word.charAt(i)))
                        temp_res.get(i).replace(word.charAt(i), temp_res.get(i).get(word.charAt(i)) + 1);
                    else
                        temp_res.get(i).put(word.charAt(i), 1);
            }
        }
        List<List<Map.Entry<Character, Integer>>> result = new ArrayList<>();
        for (var map : temp_res)
            result.add(map.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList()));
        return result;
    }

    public List<Map<Character, Double>> convertToScores(List<List<Map.Entry<Character, Integer>>> letters, int length) {
        List<Map<Character, Double>> result = new ArrayList<>();
        for (int i = 0; i < length; ++i) {
            result.add(new HashMap<>());
            for (int j = 0; j < letters.get(i).size(); ++j) {
                result.get(i).put(letters.get(i).get(j).getKey(), 1 / (26.0 - j));
            }
        }
        return result;
    }

    Map<String, Double> scoreWordsByLetters(List<String> words, Map<Character, Boolean> contains, Map<Integer, Character> correct_positions, Map<Integer, Character> incorrect_positions) {
        Map<String, Double> ranking = new HashMap<>();
        for (String word : words) {
            boolean invalid_word = false;
            if (correct_positions!=null&&correct_positions.size() > 0) {
                for (Map.Entry<Integer, Character> entry : correct_positions.entrySet()) {
                    if (!(word.charAt(entry.getKey()) == entry.getValue())) {
                        invalid_word = true;
                        break;
                    }
                }
            }
            if (incorrect_positions!=null&&incorrect_positions.size() > 0) {
                for (Map.Entry<Integer, Character> entry : incorrect_positions.entrySet()) {
                    if (word.charAt(entry.getKey()) == entry.getValue()) {
                        invalid_word = true;
                        break;
                    }
                }
            }
            if (contains!=null&&contains.size() > 0) {
                for (Map.Entry<Character, Boolean> entry : contains.entrySet()) {
                    if (word.contains("" + entry.getKey()) != entry.getValue()) {
                        invalid_word = true;
                        break;
                    }
                }
            }
            if (invalid_word) continue;
            List<Character> chars = new ArrayList<>();
            double sum = 0;
            int c_idx = -1;
            for (char c : word.toCharArray()) {
                ++c_idx;
                if (c < 'A') {
                    sum = Integer.MIN_VALUE;
                    continue;
                }
                double dynScore = dynamicLetterScores.get(c_idx).get(c);
                double statScore = letterScores[(int) c - 65];
                double wholeWordScore = dynamicLetterScoresWholeWord.get(c);
                double score = dynScore * dynamic_scores_weight + statScore * static_scores_weight+wholeWordScore*wholeWord_scores_weight;
                if (!chars.contains(c))
                    chars.add(c);
                else
                    score *= -0.1;
                sum += score;

            }
            if (sum >= 0 && !blacklist.contains(word))
                ranking.put(word, sum);
        }
        return ranking;
    }

    void debugScoring(String word, Map<Character, Boolean> contains, Map<Integer, Character> correct_positions, Map<Integer, Character> incorrect_positions) {
        if (correct_positions!=null&&correct_positions.size() > 0) {
            for (Map.Entry<Integer, Character> entry : correct_positions.entrySet()) {
                if (!(word.charAt(entry.getKey()) == entry.getValue())) {
                    System.out.println("Incorrect letter! Expected: " + entry.getValue() + " Got: " + word.charAt(entry.getKey()));
                    return;
                }
            }
        }
        if (incorrect_positions!=null&&incorrect_positions.size() > 0) {
            for (Map.Entry<Integer, Character> entry : incorrect_positions.entrySet()) {
                if (word.charAt(entry.getKey()) == entry.getValue()) {
                    System.out.println("Letter at incorrect Position! Letter: " + word.charAt(entry.getKey()));
                    return;
                }
            }
        }
        if (contains!=null&&contains.size() > 0) {
            for (Map.Entry<Character, Boolean> entry : contains.entrySet()) {
                if (word.contains("" + entry.getKey()) != entry.getValue()) {
                    if (entry.getValue())
                        System.out.println("Does not contain " + entry.getKey() + "!");
                    else
                        System.out.println("Does contain " + entry.getKey() + "!");
                    return;
                }
            }
        }
    }

    public void solve() throws IOException {
        List<String> words = readWordsOfLength("english3.txt", 5);

        Map<Character, Boolean> contains = new HashMap<>();
        Map<Integer, Character> correct_positions = new HashMap<>();
        Map<Integer, Character> incorrect_positions = new HashMap<>();
        Random rnd = new Random();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        for (int tryNr = 0; tryNr < 10; tryNr++) {
            dynamicLetterScores = convertToScores(countLetters(words, 5), 5);
            dynamicLetterScoresWholeWord = convertToScoresAllLetters(countLettersAllPositions(words,5));
            Map<String, Double> scored_words = scoreWordsByLetters(words, contains, correct_positions, incorrect_positions);
            words = new ArrayList<>(scored_words.keySet());
            List<Map.Entry<String, Double>> ranked = scored_words.entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue)).collect(Collectors.toList());
            if (ranked.size() == 0) {
                System.out.println("No Word Found.\nEnter correct answer:");
                String result = reader.readLine();
                debugScoring(result, contains, correct_positions, incorrect_positions);
                return;
            }
            int best = Collections.frequency(scored_words.values(), ranked.get(ranked.size() - 1).getValue());

            Map.Entry<String, Double> bestWord = ranked.get(ranked.size() - 1 - rnd.nextInt(best));
            System.out.println("-----------------");
            for (int i = (Math.max(best, 5)); i > 0; --i)
                if (ranked.size() - i > -1)
                    System.out.println(ranked.get(ranked.size() - i).getKey() + " - " + ranked.get(ranked.size() - i));
            System.out.println("-----------------");
            System.out.println("Try word " + bestWord.getKey() + " (Score: " + bestWord.getValue() + ", Possible words: " + ranked.size() + ")");
            System.out.println("Enter Result:\n0 = Not in word | 1 = In word, different position | 2 = Correct position");

            String result = reader.readLine();
            if (!result.equalsIgnoreCase("BLACKLIST")) {
                if (result.equals("22222")) {
                    System.out.println("Congrats!");
                    return;
                }
                blacklist.add(bestWord.getKey());
                for (int i = 0; i < 5; ++i) {
                    char res = result.charAt(i);
                    char word_c = bestWord.getKey().charAt(i);

                    if (res == '0') {
                        if (!contains.containsKey(word_c))
                            contains.put(word_c, false);
                    } else {
                        contains.put(word_c, true);
                        if (word_c == 'M')
                            System.out.println("Set M to True");
                        if (res == '2')
                            correct_positions.put(i, word_c);
                        else
                            incorrect_positions.put(i, word_c);
                    }
                }
                //System.out.println("Got result: " + result);
            } else {
                blacklist.add(bestWord.getKey());
                words = readWordsOfLength("english3.txt", 5);
                tryNr--;
                System.out.println("Added " + bestWord.getKey() + " to blacklist");
            }
        }
    }
}
