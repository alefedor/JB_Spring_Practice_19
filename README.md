# Parallel Dijkstra Algorithm

В рамках этого тестового задания вам необходимо реализовать параллельную версию алгоритма Дейкстры для поиска кратчайшего пути в графе. Файл `Graph.kt` содержит классы `Node` и `Edge`, описывающие граф. Вы можете использовать поле `Node.distance` для того, чтобы хранить расстояние до текущей вершины во время поиска; функция `clearNodes(..)` сбрасывает эти расстояния на `Int.MAX_VALUE`.

Реализация параллельной весии должна содержаться в функции `shortestPathSequential` в файле `Dijkstra.kt`. Там находится некоторый скелет для будушего алгоритма, но его использование не обязательно и никак не влияет на оценивание.

В качестве многопоточной приоритетной очереди предлагается использовать обычную бинарную кучу с глобальной блокировкой. Вы так же можете изменять класс `Node` по своему усмотрению (добавить блокировку для синхронизации, дополнительную информацию для поддержки операции `decreaseKey`, сделать возможным использование `CAS` на поле `distance` и так далее). Предполагается, что пока очередь пуста, но работа ещё не окончена, worker-ы могут крутиться в цикле в ожидании нового элемента или окончания работы и кушать CPU.