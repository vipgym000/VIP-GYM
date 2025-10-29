package SD_Tech.VipGym.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import SD_Tech.VipGym.Entity.Membership;
import SD_Tech.VipGym.Entity.User;
import SD_Tech.VipGym.Entity.UserStatus;
import SD_Tech.VipGym.Repository.UserRepository;
import jakarta.transaction.Transactional;

@Service
public class PendingPaymentScheduler {

    @Autowired
    private UserRepository userRepository;

    /**
     * ✅ Runs every day at midnight.
     * - Checks all ACTIVE users.
     * - If due date is passed, adds membership fee once to pending amount.
     * - If a membership switch is pending, activates it after the due date.
     */
    @Scheduled(cron = "0 0 0 * * *") // runs every midnight
    @Transactional
    public void updatePendingPayments() {
        System.out.println("🔄 Checking for ACTIVE users with expired due dates...");

        List<User> users = userRepository.findAll();
        LocalDate today = LocalDate.now();

        for (User user : users) {
            if (user.getStatus() != UserStatus.ACTIVE) continue;
            if (user.getMembership() == null || user.getNextDueDate() == null) continue;

            boolean duePassed = user.getNextDueDate().isBefore(today) || user.getNextDueDate().isEqual(today);

            if (!duePassed) continue;

            // ✅ If membership switch pending and due date passed — activate new plan
            if (user.isMembershipSwitchPending() && user.getNextMembership() != null) {

                Membership newPlan = user.getNextMembership();
                user.setMembership(newPlan);
                user.setNextMembership(null);
                user.setMembershipSwitchPending(false);

                // ✅ Add the new plan fee
                user.setPendingAmount(user.getPendingAmount() + newPlan.getFee());

                // ✅ Extend due date according to new plan
                user.setNextDueDate(today.plusMonths(newPlan.getDurationInMonths()));

                System.out.println("🔁 Switched plan for user: " + user.getFullName() +
                        " | New Plan: " + newPlan.getName() +
                        " | Pending +₹" + newPlan.getFee() +
                        " | Next Due: " + user.getNextDueDate());

            } else {
                // ✅ Normal renewal (no pending switch)
                double fee = user.getMembership().getFee();
                user.setPendingAmount(user.getPendingAmount() + fee);
                user.setNextDueDate(user.getNextDueDate().plusMonths(user.getMembership().getDurationInMonths()));

                System.out.println("⚠️ Renewed plan for user: " + user.getFullName() +
                        " | +₹" + fee +
                        " | New Pending: ₹" + user.getPendingAmount() +
                        " | Next Due: " + user.getNextDueDate());
            }
        }

        userRepository.saveAll(users);
        System.out.println("✅ Membership dues and pending switches processed successfully.");
    }
}
