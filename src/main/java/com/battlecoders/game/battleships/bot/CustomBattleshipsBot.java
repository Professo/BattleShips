package com.battlecoders.game.battleships.bot;

import java.util.List;
import java.util.Random;

import com.battlecoders.exception.UnacceptableStorageHelperOperation;
import com.battlecoders.game.battleships.bot.ShotVector.VectorType;
import com.battlecoders.game.battleships.bot.entity.Board;
import com.battlecoders.game.battleships.bot.entity.Cell;
import com.battlecoders.game.battleships.bot.entity.CellState;
import com.battlecoders.game.battleships.bot.entity.InitHelper;
import com.battlecoders.game.battleships.bot.entity.Ship;
import com.battlecoders.game.battleships.bot.entity.Shot;
import com.battlecoders.game.bot.StorageHelper;

public class CustomBattleshipsBot extends BattleshipsBot {

    private final static String VECTOR_KEY = "vector";

    @Override
    protected Shot doTurn(Board board, StorageHelper storageHelper) {
        Shot resultShot = null;

        // загружаем сохраненные данные
        String vectorValue = storageHelper.getProperty(VECTOR_KEY);
        ShotVector vector = parseVector(vectorValue);

        // если вектор = null -> это первый выстрел и нам все равно куда стрелять
        // генирим случайный корректный выстрел на его основе и создаем вектор.
        if (vector == null) {
            resultShot = getRandomShot(board);
            vector = createNewVector(resultShot);
        } else {
            // если вектор существует, в данном случае это может быть начальный вектор, первый выстрел, или тот, который имеет определенное направление
            // в любом случае нам необходимо определить результат последнего выстрела
            Shot lastShot = vector.getLastShot() != null ? vector.getLastShot() : vector.getStartShot();

            // получаем статус последнего выстрела
            CellState state = board.getCellState(lastShot.getX(), lastShot.getY());

            // в лог пишем debug информацию, исключительно для удобства отладки
            storageHelper.addLog("start >> " + shotToString(lastShot) + "> " + state.name());

            // Определяем статус последнего выстрела
            // если мы потопили корабль или просто промазали( при первом выстреле)
            // просто генирим следующий случайный выстрел и новый вектор
            if (CellState.SUNK.equals(state) || (CellState.MISS.equals(state) && vector.getLastShot() == null)) {
                resultShot = getRandomShot(board);
                vector = createNewVector(resultShot);
            } else {
                // если корабль все-таки подбит
                // если мы промазали, это означает, что в данном направлении вектора палубы корабля закончились
                // и нам необходимо развернуть вектор и начать обстрел в другом направлении
                if (CellState.MISS.equals(state)) {

                    // рассчитываем координаты для выстрела в обратном направлении (разворот вектора на 180 градусов)
                    Shot preFirstShot = getNextShotByVector(vector.getStartShot(), (vector.getCurrentDirection() * -1), vector.getVectorType(), board, storageHelper);

                    // если данный выстрел невозможен => в данной плоскости стрелять некуда и нам необходимо развернуть вектор на 90 градусов
                    // даная ситуация происходит в том случае, если мы подбили первую палубу, а на 2 выстреле промазали
                    if (preFirstShot == null || !board.isCellState(preFirstShot.getX(), preFirstShot.getY(), CellState.EMPTY)) {
                        VectorType nType = VectorType.X.equals(vector.getVectorType()) ? VectorType.Y : VectorType.X;
                        int currentDirection = vector.getCurrentDirection();
                        resultShot = getNextShotByVector(vector.getStartShot(), currentDirection, nType, board, storageHelper);

                        if (resultShot == null) {
                            currentDirection = currentDirection * -1;
                            resultShot = getNextShotByVector(vector.getStartShot(), currentDirection, nType, board, storageHelper);
                        }

                        vector = new ShotVector(nType, vector.getStartShot(), resultShot, currentDirection);
                    } else {

                        // если мы можем поменять направление вектора на 180 градусов
                        // обновляем вектор с учетом нового выстрела
                        resultShot = preFirstShot;
                        vector = new ShotVector(vector.getVectorType(), vector.getStartShot(), resultShot, (vector.getCurrentDirection() * -1));
                    }

                } else if (CellState.WOUNDED.equals(state)) {

                    // если мы попали
                    // у нас есть 2 варианта, либо это был первый выстрел по данному кораблю, либо вектор сформирован
                    // в любом случае необходимо определить следующий возможный выстрел

                    VectorType nType = vector.getVectorType();
                    int currentDirection = vector.getCurrentDirection();

                    //проверяем координаты по направлению вектора
                    resultShot = getNextShotByVector(lastShot, currentDirection, vector.getVectorType(), board, storageHelper);

                    if (resultShot == null) {
                        // разворачиваем вектор на 180
                        currentDirection = currentDirection * -1;
                        resultShot = getNextShotByVector(vector.getStartShot(), currentDirection, vector.getVectorType(), board, storageHelper);
                    }

                    // change vector
                    if (resultShot == null) {
                        // разворачиваем на 90
                        nType = VectorType.X.equals(vector.getVectorType()) ? VectorType.Y : VectorType.X;
                        currentDirection = vector.getCurrentDirection();
                        resultShot = getNextShotByVector(vector.getStartShot(), currentDirection, nType, board, storageHelper);
                        if (resultShot == null) {
                            // разворачиваем на 180
                            currentDirection = currentDirection * -1;
                            resultShot = getNextShotByVector(vector.getStartShot(), currentDirection, nType, board, storageHelper);
                        }
                    }

                    // формируем результирующий вектор
                    vector = new ShotVector(nType, vector.getStartShot(), resultShot, currentDirection);
                }
            }
        }

        // сохраняем значение вектора
        try {
            storageHelper.saveProperty(VECTOR_KEY, vectorToString(vector));
        } catch (UnacceptableStorageHelperOperation unacceptableStorageHelperOperation) {
            storageHelper.addLog("Error >> " + unacceptableStorageHelperOperation.getMessage());
        }
        storageHelper.addLog("Result >> " + shotToString(resultShot));
        return resultShot;
    }

    private Shot getNextShotByVector(Shot startPoint, int vectorWay, VectorType vectorType, Board board, StorageHelper storageHelper){
        int x = startPoint.getX();
        int y = startPoint.getY();

        if(VectorType.X.equals(vectorType)){
            x += vectorWay;
        }else{
            y += vectorWay;
        }
        Shot result = new Shot(x, y);
        storageHelper.addLog(shotToString(startPoint) + ">>" + shotToString(result) + ">>" + isShotInBoard(result, board));
        return isShotInBoard(result, board) ? result : null;
    }


    private ShotVector createNewVector(Shot shot){
        return new ShotVector(VectorType.X, shot, null, 1);
    }

    private ShotVector parseVector(String vectorValue) {
        if(vectorValue != null){
            String[] vectorData = vectorValue.split(";");
            String name = vectorData[0];
            Shot startShot = parseShot(vectorData[1]);
            Shot currentShot = parseShot(vectorData[2]);
            int currentDirection = Integer.valueOf(vectorData[3]);
            return new ShotVector(VectorType.valueOf(name), startShot, currentShot, currentDirection);
        }
        return null;
    }

    private String vectorToString(ShotVector vector){
        return vector.getVectorType().name()+";"+ shotToString(vector.getStartShot())+ ";" +
                shotToString(vector.getLastShot()) + ";" + vector.getCurrentDirection();
    }

    private boolean isShotInBoard(Shot shot, Board board){
        return shot.getX() >=0 && shot.getY() >=0 && shot.getX() < board.getHeight() && shot.getY() < board.getWidth() && board.isShotValid(shot);
    }

    private Shot parseShot(String data){
        if(data != null && !data.trim().isEmpty()){
            String[] res = data.trim().split(":");
            return new Shot(Integer.parseInt(res[0]), Integer.parseInt(res[1]));
        }
        return null;
    }

    private String shotToString(Shot value){
        return value == null ? "" : (value.getX()+":"+value.getY());
    }

    private Shot getRandomShot(Board board){
        Random randomGenerator = new Random();
        List<Cell> emptyCells = board.getCells(CellState.EMPTY);
        while (emptyCells.size() > 0) {
            int index = randomGenerator.nextInt(emptyCells.size());
            Shot shot = new Shot(emptyCells.get(index).getX(), emptyCells.get(index).getY());
            if (board.isShotValid(shot)) {
                return shot;
            } else {
                emptyCells.remove(index);
            }
        }
        return null;
    }

    @Override
    protected List<Ship> doInit(InitHelper helper, StorageHelper storageHelper) {
        Random randomGenerator = new Random();

        while (!helper.isComplete()) {
            for (int shipSize = 4; shipSize > 0; --shipSize) {
                for (int shipSizeCount = 5 - shipSize; shipSizeCount > 0; --shipSizeCount) {
                    while (true) {
                        int x = randomGenerator.nextInt(helper.getBoardWidth());
                        int y = randomGenerator.nextInt(helper.getBoardHeight());
                        int orientationCode = randomGenerator.nextInt(2);
                        Ship ship = new Ship(shipSize, x, y, Ship.ShipOrientation.values()[orientationCode]);
                        if (helper.putShip(ship)) {
                            break;
                        }
                    }
                }
            }
        }

        return helper.getShips();
    }
}
