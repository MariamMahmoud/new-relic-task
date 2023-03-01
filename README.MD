<!-- ### My thoughts and thought process

- It made sense to me that later in the interview we would test on may many metric types and on a bigger file
- So it made sense that it had to be as generic as possible to adapt to any set of valid inputs
- The if conditions and the hard coded metric names were definatly a code smell
- At first I understood the problem, did minor refactor and then got a big commit with a huge refactor all at once
- It produced correct results but not sorted
- I wasted some time trying to sort it or figure out why the sort got messed up, with no luck!
- I pushed the code just to feel better about having to deliver something that is working.
- OFC the run time was not lesser after parallel execution, but that's ok, as the input is small so the overhead of creating and merging threads would waste more time that what the threading would save. 
- I expect better performance as the input grows.
- Now back to the sorting problem.
-  I started over, trying to find the point in time, the line of code, the change that messed up the sorting.
- One small, very small change at a time. now I have an idea how it should look like at the end thanks to the messey solution attempt.
- Test, if all is well then push.
- If sort is messed, revert.
- Try another idea of refactoring or refactor a different area and drop the stubborn one for now.
- I noticed everytime I combine the aggregation followed by the averaging methods, sorting is messed.
- As if averraging is resisting to be in the same place with sorting!!
- (probably I needed to use Map.of(key, value) and this was the problem.)
- Anyway I did the aggregation and the averaging in 2 different (each in parallel) loop.
- It worked! Still smelly, why 2 loops doing almost the same thing
- It won't change the time complexity, same Big(O) yet it is redundant
- Finally I kept iterating around it, making every possible optimization till combining the loops was the only thing left.
- I added a TODo, just to mark it, then tried one last time.
- It worked!!! Sorted and all
- What's the diff, I need to compare the 3rd commit (the messy one) with the final one
- Also I need to check for more areas of improvement.

### Learnings
- Small Commits and test
- Divide, conquer, test:
    - Drop stubborn code, conquer less stubborn ones
    - Break big problems, solve them in rotations, one optimization at a time -->
